# Cargo support

## Overview  

[detect_product_short] includes two Cargo detectors:

* **Cargo CLI Detector** 
* **Cargo Lockfile Detector**  

## Cargo CLI Detector  

* Detector for Cargo projects, extracts **direct and transitive dependencies** using the [cargo tree](https://doc.rust-lang.org/cargo/commands/cargo-tree.html) command.

**Requirements:**  
* A valid `cargo` executable must be available on the system.
* A `Cargo.toml` file must be present in the project.
* Cargo version 1.44.0+ required to support `cargo tree`.

**Behavior:**  
* Executes `cargo tree` to analyze dependencies.  
* Parses the output to construct a hierarchical **dependency graph**.  
* Falls back to the Cargo Lockfile Detector if `cargo tree` is unavailable.

To specify a custom Cargo executable path, use the `detect.cargo.path` property.  

## Cargo Lockfile Detector  

If `cargo tree` is unavailable, [detect_product_short] will default to the Cargo Lockfile Detector, which extracts dependencies by parsing the `Cargo.lock` file.

* If both `Cargo.toml` and `Cargo.lock` are present, the detector uses `Cargo.lock` for dependency information.  
* If `Cargo.lock` is missing, the detector prompts the user to generate it using `cargo generate-lockfile`.  
* Extracts the project’s name and version from `Cargo.toml`. If missing, it derives values from Git or the project directory.

## Cargo Workspaces

[detect_product_short] supports Cargo workspaces allowing multiple related Rust crates to be managed together under a single root. All workspace members share a single `Cargo.lock` file and a common `target/` directory. For more information, see the [Cargo Workspaces documentation](https://doc.rust-lang.org/cargo/reference/workspaces.html).

**Behavior:**
* [detect_product_short] identifies Cargo workspaces declared via `[workspace]` in the root `Cargo.toml`.
* Virtual workspaces (root with `[workspace]` but no `[package]`) include dependencies from all workspace members by default.
* Non-virtual workspaces (root with both `[workspace]` and `[package]`) include only root package dependencies by default.
* Additional configuration enables inclusion of workspace member dependencies in both cases.
* Reads and interprets the shared `Cargo.lock` at the workspace root for Cargo Lockfile Detector.

**Configuration Options:**
* Use `detect.cargo.ignore.all.workspaces = true` to ignore all workspaces.
* Use `detect.cargo.included.workspaces` to specify a comma-separated list of workspaces to include (by workspace name).
* Use `detect.cargo.excluded.workspaces` to specify a comma-separated list of workspaces to exclude (by workspace name).

## Cargo Features and Optional Dependencies

[detect_product_short] supports Cargo features and optional dependencies for the Cargo CLI Detector. These properties control which features are enabled when running `cargo tree`.

**Configuration Options:**
* `detect.cargo.included.features` - Specify which features to include. Accepts:
    * A comma-separated list of specific features (e.g., `feature-a,feature-b`)
    * `ALL` to enable all features
    * `NONE` to disable all features
    * Unset/blank (default) to use only default features
* `detect.cargo.disable.default.features` - Set to `true` to disable default features.

`<note type="note">Feature control is only supported by the Cargo CLI Detector. The Cargo Lockfile Detector does not support this feature and will log a warning if the parameter is specified.</note>`

## Orphan Dependencies in Cargo Lockfile Detector (pre-11.2.0)

`<note type="note">This behavior applies to versions prior to 11.2.0. As of 11.2.0 workspace support resolves most orphan dependency scenarios.</note>`

In older versions of Cargo projects, it was possible for `Cargo.lock` to contain packages that were not explicitly declared in `Cargo.toml`. These were referred to as **orphan dependencies**.

Since orphan dependencies could not be mapped to a specific dependency path in the graph, the **Cargo Lockfile Detector** did not discard them. Instead, they were grouped under a placeholder component named **`Additional_Components`**, which appeared as a direct dependency in the graph.

This approach ensured that all components listed in `Cargo.lock` were included in the BOM, even if their exact relationship within the dependency tree could not be determined.
