#!/bin/bash

set -e

CURRENTBRANCH=$(git status|awk 'NR==1{print $3}');

if [ ! "$CURRENTBRANCH"=="master" ]; then
    echo -e "Not on master - cannot proceed."
    exit 1
fi

LOCAL=$(git rev-parse @)
REMOTE=$(git rev-parse @{u})

if [ "$LOCAL" != "$REMOTE" ]; then
    echo "Your working branch has diverged from the remote master, cannot continue"
    exit 1
fi

lein clean

lein test

lein vcs assert-committed
lein change version "leiningen.release/bump-version" release
lein vcs commit
lein vcs tag
lein deploy clojars
lein change version "leiningen.release/bump-version"
lein vcs commit
git push origin master
git push --tags origin
