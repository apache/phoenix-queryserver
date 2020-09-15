#!/usr/bin/env bash
REPO_ROOT=$1
for linksource in $(find $REPO_ROOT -name \*.jar); do
    linkfile=$(basename $linksource)
    linkdir=$(dirname $linksource)
    targetdir=$(realpath $REPO_ROOT/.. --relative-to=$linkdir)
    target="$targetdir/$linkfile"
    cd $linkdir
    #The copy is necessary, as maven won't add dangling symlinks to the assmebly
    cp $linkfile $target
    ln -sf $target $linkfile
done