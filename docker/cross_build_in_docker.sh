#!/usr/bin/env bash

# SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
#
# SPDX-License-Identifier: Apache-2.0

set -euo pipefail

z3_version="$1"
target_uid="$2"
target_gid="$3"

apt-get update
apt-get install -y python3 make gcc-10-aarch64-linux-gnu g++-10-aarch64-linux-gnu unzip
wget "https://github.com/Z3Prover/z3/archive/refs/tags/z3-${z3_version}.zip"
unzip "z3-${z3_version}.zip"
cd "z3-z3-${z3_version}"
# See https://github.com/Z3Prover/z3/blob/c7af97364a55394396b69feb04c449aa3fe60f1f/scripts/mk_unix_dist.py#L138-L146
ln -s /usr/bin/aarch64-linux-gnu-gcc-10 /usr/bin/aarch64-none-linux-gnu-gcc
ln -s /usr/bin/aarch64-linux-gnu-g++-10 /usr/bin/aarch64-none-linux-gnu-g++
export CC=aarch64-none-linux-gnu-gcc
export CXX=aarch64-none-linux-gnu-g++
export AR=aarch64-linux-gnu-gcc-ar-10
export NM=aarch64-linux-gnu-gcc-nm-10
# See https://docs.aws.amazon.com/linux/al2023/ug/performance-optimizations.html
export CFLAGS="-flto=auto -O2 -march=armv8.2-a+crypto -mtune=neoverse-n1 -ftree-vectorize"
export CXXFLAGS="${CFLAGS}"
export LDFLAGS="-flto=auto -O2"
python3 scripts/mk_unix_dist.py -f --nodotnet --arch=arm64
cp --preserve=all "./dist/z3-${z3_version}-arm64-glibc-2.31/bin"/*.so /data/out/
chown "${target_uid}:${target_gid}" /data/out/*
