Releasing a new version
=======================

Refer to the Phoenix release docs https://phoenix.apache.org/release.html for additinal information

Pre-requisites
--------------

You need to be a Phoenix PMC to be able to upload the RC and final releases to dist.apache.org

Make sure your gpg is set up, and that the default key is your code signing key.
See http://www.apache.org/dev/release-signing.html

Make sure that your git remote ``origin`` points to either the main gitbox or the main github
phoenix-queryserver repo.

Make sure that you have a PyPI account, and that you can publish to the
https://pypi.org/project/phoenixdb/ project. If not, then reach out to one of the maintainers listed there for permission.

For instructions on the PyPi registration and publishing process, see
https://kynan.github.io/blog/2020/05/23/how-to-upload-your-package-to-the-python-package-index-pypi-test-server

Prepare the RC
--------------

#. Make sure the dockerized tests described in README.rst run successfully

#. Make sure to run twine check on the phoenixdb package files for python 2 and 3 and ensure they pass:

    python setup.py sdist bdist_wheel
    twine check dist/*

#. Discuss release plans on dev@phoenix.a.o

#. Open a ticket like https://issues.apache.org/jira/browse/PHOENIX-6529

#. Change the version number in ``setup.py`` and ``NEWS.rst``.

#. Add the changes since the last release to ``NEWS.rst``

#. Make a PR form the changes get it reviewed, and commit it

#. Run the dev_support/make_rc.sh script, and choose the option to tag the release::

    cd python-phoenixdb
    ./dev-support/make_rc.sh

#. The distribution will be generated under the python-phoenixdb/release directory. Upload the directory to https://dist.apache.org/repos/dist/dev/phoenix/ with SVN::

    cd workdir
    svn co https://dist.apache.org/repos/dist/dev/phoenix --depth empty
    cd phoenix
    cp -r <build-dir>/python-phoenixdb/release/<release-dir> .
    svn add <release-dir>
    svn commit

Voting
------

#. Follow the Voting section in https://phoenix.apache.org/release.html

You can use http://mail-archives.us.apache.org/mod_mbox/phoenix-dev/202108.mbox/%3CCAJ0%2BiOs2P8EQq_GEGwb%2BVyWur_HyvUGRgVvrD55Xh249QNUcNQ%40mail.gmail.com%3E
as an email template.

Publishing
----------

#. If the vote passes, upload the package to PyPI (using the instructions linked above, and some extra notes here)
    * Make sure to run the ``python setup.py sdist bdist_wheel`` with python 2 and 3 as well.
    * To verify it you should have two ``.whl`` files under ``dist/`` folder
    * ``pip install --index-url https://test.pypi.org/simple/ phoenixdb`` might not work for python2 because the test repo tends to miss the older versions of dependencies required for Python2.

#. Bump the package version to <major>.<minor>.<patch>.dev0 in ``setup.py``, and commit the change

#. Follow the steps from the ``Release`` section in https://phoenix.apache.org/release.html , but skip the following steps:
    * maven release
    * new branch creation
    * mvn version set

Congratulations!