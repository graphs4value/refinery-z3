<!--
  SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>

  SPDX-License-Identifier: Apache-2.0
-->

# Refinery Z3 Java Bindings

[![Build](https://github.com/graphs4value/refinery-z3/actions/workflows/build.yml/badge.svg)](https://github.com/graphs4value/refinery-z3/actions/workflows/build.yml)

The repository contains Java bindings for
[Z3](https://microsoft.github.io/z3guide/) for use in the
[Refinery](https://refinery.tools) project.

Some code in this repository has been adapter from
[Google OR-TOOLS](https://developers.google.com/optimization/), which is
available under
[The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

The artifacts produced in the repository repackage upstream Z3 releases as
Maven modules. As an exception, we build binaries for _Linux aarch64_,
because Z3 does not offer `glibc 2.31` compatible binaries for _aarch64_, but
only for _x64_. Refinery uses
[Amazon Linux 2023 Minimal](https://docs.aws.amazon.com/linux/al2023/ug/core-glibc.html)
for its container images, which only supports `glibc 2.34`, but not
`glibc 2.35` as required by upstream Z3 binaries.

## License

Copyright (c) 2023-2024 [The Refinery Authors](https://github.com/graphs4value/refinery/blob/main/CONTRIBUTORS.md)

Z3 is available under the [MIT License](https://raw.githubusercontent.com/Z3Prover/z3/master/LICENSE.txt).

Refinery Z3 Java Bindings are available under [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt).

Refinery complies with the [REUSE Specification – Version 3.0](https://reuse.software/) to provide copyright and licensing information to each file, including files available under other licenses.
For more information, see the comments headers in each file and the license texts in the [LICENSES](LICENSES/) directory.
