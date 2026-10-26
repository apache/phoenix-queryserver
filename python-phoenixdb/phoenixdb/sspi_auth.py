# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""SPNEGO authentication for requests on Windows.

Uses pyspnego, which goes through the native SSPI (and so the logged on user's
LSA Kerberos tickets) on Windows, so no MIT Kerberos installation is needed.
"""

import base64
import re
from urllib.parse import urlparse

from requests.auth import AuthBase

try:
    import spnego
except OSError as e:
    # pyspnego only catches ImportError around its optional gssapi import. A gssapi
    # package installed without MIT Kerberos for Windows raises OSError instead.
    raise RuntimeError(
        "pyspnego could not be imported because the installed 'gssapi' package is "
        "broken (%s). SSPI does not need it: run 'pip uninstall gssapi requests-gssapi'."
        % e) from e

_NEGOTIATE_RE = re.compile(r'(?:^|,)\s*Negotiate\s*([^,]*)', re.IGNORECASE)


def _negotiate_token(response):
    match = _NEGOTIATE_RE.search(response.headers.get('www-authenticate', ''))
    if match is None:
        return None
    token = match.group(1).strip()
    return base64.b64decode(token) if token else b''


class HTTPSPNEGOAuthSSPI(AuthBase):
    """Opportunistic SPNEGO ("Negotiate") authentication, like
    requests_gssapi.HTTPSPNEGOAuth(opportunistic_auth=True) with mutual
    authentication disabled."""

    def __init__(self, service='HTTP'):
        self.service = service

    def _new_context(self, url):
        return spnego.client(hostname=urlparse(url).hostname, service=self.service,
                             protocol='negotiate')

    def __call__(self, r):
        ctx = self._new_context(r.url)
        r.headers['Authorization'] = 'Negotiate ' + base64.b64encode(ctx.step()).decode()
        r.register_hook('response', self._handle_401(ctx))
        return r

    def _handle_401(self, ctx):
        def handle(response, **kwargs):
            if response.status_code != 401 or getattr(response.request, '_spnego_retried', False):
                return response
            in_token = _negotiate_token(response)
            if not in_token or ctx.complete:
                # The server rejected the token outright, retrying won't help
                return response

            # Multi-leg exchange: answer the server's challenge on the same connection
            response.content
            response.close()
            prep = response.request.copy()
            prep.headers['Authorization'] = 'Negotiate ' + \
                base64.b64encode(ctx.step(in_token)).decode()
            prep._spnego_retried = True
            new_response = response.connection.send(prep, **kwargs)
            new_response.history.append(response)
            new_response.request = prep
            return new_response
        return handle
