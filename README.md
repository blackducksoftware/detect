## Overview

Black Duck Detect scans code bases in your projects and folders to perform compositional analysis and functions as an intelligent scan client for Black Duck SCA products. Black Duck Detect can send scan results to Black Duck, which generates risk analysis when identifying open source components, licenses, and security vulnerabilities.

## Build

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=com.blackduck.integration%3Adetect&metric=alert_status)](https://sonarcloud.io/dashboard?id=com.blackduck.integration%3Adetect)

## Where can I get the latest release?

*Available from GitHub for Linux/MacOS by running:*

```bash
bash <(curl -s -L https://detect.blackduck.com/detect.sh)
```

*Available from GitHub for Windows by running in **command prompt**:*

```cmd
powershell "[Net.ServicePointManager]::SecurityProtocol = 'tls12'; irm https://detect.blackduck.com/detect.ps1?$(Get-Random) | iex; detect"
```

*Available from GitHub for Windows/Linux by running in **powershell**:*
```powershell
[Net.ServicePointManager]::SecurityProtocol = 'tls12'; $Env:DETECT_EXIT_CODE_PASSTHRU=1; irm https://detect.blackduck.com/detect.ps1?$(Get-Random) | iex; detect
```

For scripts, please see [Detect Scripts](https://detect.blackduck.com).

For AirGap, please use our [Repo](https://repo.blackduck.com/bds-integrations-release/com/blackduck/integration/detect/).

## Documentation

The latest quickstart documentation is [here](https://documentation.blackduck.com/bundle/detect/page/gettingstarted/quickstart.html).

The latest full documentation is [here](https://documentation.blackduck.com/bundle/detect/page/introduction.html).

## Getting help

Additional information and help is available from the
[Black Duck Software Community](https://community.blackduck.com/s/ ).
