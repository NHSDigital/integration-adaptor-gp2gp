#!/bin/bash

clean_tag_element() {
    local tag_element="$1"
    echo "${tag_element//\//-}"
}

generate_tag() {
    local clean_branch_name=$(clean_tag_element "$1")
    local clean_build_id=$(clean_tag_element "$2")
    local git_hash="$3"

    local tag="${clean_branch_name}-${clean_build_id}-${git_hash:0:7}"

    echo "$tag"
}

if [[ $# -ne 3 ]]; then
    echo "Usage: $0 branch_name build_id git_hash"
    exit 1
fi

branch_name="$1"
build_id="$2"
git_hash="$3"

generate_tag "$branch_name" "$build_id" "$git_hash"
