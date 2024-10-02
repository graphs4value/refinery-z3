#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

z3_version="$1"
target_uid="$2"
target_gid="$3"

apt-get update
apt-get install -y python3 make gcc-10 g++-10 unzip
wget "https://github.com/Z3Prover/z3/archive/refs/tags/z3-${z3_version}.zip"
unzip "z3-${z3_version}.zip"
cd "z3-z3-${z3_version}"
export CC=gcc-10
export CXX=g++-10
export AR=gcc-ar-10
export NM=gcc-nm-10
# See https://docs.aws.amazon.com/linux/al2023/ug/performance-optimizations.html
export CFLAGS="-flto=auto -O2 -march=x86-64-v2 -ftree-vectorize"
export CXXFLAGS="${CFLAGS}"
export LDFLAGS="-flto=auto -O2"
python3 scripts/mk_unix_dist.py -f --nodotnet
cp --preserve=all "./dist/z3-${z3_version}-x64-glibc-2.31/bin"/*.so /data/out/
chown "${target_uid}:${target_gid}" /data/out/*
