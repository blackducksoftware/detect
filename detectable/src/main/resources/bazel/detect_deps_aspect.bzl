# detect_deps_aspect.bzl
# Black Duck Detect — dependency metadata aspect (Bazel 8+, Bzlmod)
#
# Traverses deps / runtime_deps / data edges of the target's BUILD graph.
# At each node, writes a small JSON file with the target's metadata and its
# direct dep edges. Detect reads all these files and reconstructs the graph.
#
# Invoke:
#   bazel build //your:target \
#     --aspects=//:detect_deps_aspect.bzl%detect_deps_aspect \
#     --output_groups=dep_info

DetectDepsInfo = provider(
    doc = "Transitive set of dependency metadata files produced by detect_deps_aspect.",
    fields = {
        "transitive_data": "Depset of JSON files, one per visited target.",
    },
)

# ── Coordinate extractors ────────────────────────────────────────────────────

def _maven_coordinates(tags):
    """rules_jvm_external: tags = ['maven_coordinates=g:a:v', ...]"""
    for tag in tags:
        if tag.startswith("maven_coordinates="):
            return tag[len("maven_coordinates="):]
    return None

def _crate_name(tags):
    """rules_rust/crates_repository: tags = ['crate-name=foo', ...]"""
    for tag in tags:
        if tag.startswith("crate-name="):
            return tag[len("crate-name="):]
    return None

def _pypi_name(tags):
    """rules_python/pip.parse: tags = ['pypi_name=requests', ...]"""
    for tag in tags:
        if tag.startswith("pypi_name="):
            return tag[len("pypi_name="):]
    return None

def _pypi_version(tags):
    """rules_python/pip.parse: tags = ['pypi_version=2.31.0', ...]"""
    for tag in tags:
        if tag.startswith("pypi_version="):
            return tag[len("pypi_version="):]
    return None

# ────────────────────────────────────────────────────────────────────────────

def _detect_deps_impl(target, ctx):
    # Collect direct dep edges across the three attributes we follow.
    direct_deps = []
    for attr_name in ["deps", "runtime_deps", "data"]:
        for dep in getattr(ctx.rule.attr, attr_name, []):
            direct_deps.append({
                "label": str(dep.label),
                "attribute": attr_name,
            })

    tags = getattr(ctx.rule.attr, "tags", [])

    metadata = {
        "label": str(target.label),
        "kind": ctx.rule.kind,
        "direct_deps": direct_deps,
    }

    # Maven — rules_jvm_external stamps maven_coordinates=g:a:v on jvm_import targets
    coords = _maven_coordinates(tags)
    if coords:
        metadata["maven_coordinates"] = coords

    # Rust — crates_repository stamps crate-name= tag + version attribute on rust_library targets
    crate = _crate_name(tags)
    if crate:
        metadata["crate_name"] = crate
        version = getattr(ctx.rule.attr, "version", None)
        if version:
            metadata["crate_version"] = version

    # Python — pip.parse stamps pypi_name= and pypi_version= tags on py_library targets
    pypi_pkg = _pypi_name(tags)
    pypi_ver = _pypi_version(tags)
    if pypi_pkg:
        metadata["pypi_name"] = pypi_pkg
    if pypi_ver:
        metadata["pypi_version"] = pypi_ver

    # Go — rules_go sets importpath on go_library/go_binary targets; no version on the target itself
    importpath = getattr(ctx.rule.attr, "importpath", None)
    if importpath:
        metadata["go_importpath"] = importpath

    # One JSON file per target — target name in filename avoids package-level collisions.
    out = ctx.actions.declare_file("_detect_dep_meta_{}.json".format(target.label.name))
    ctx.actions.write(out, json.encode(metadata))

    # Bubble this file + all transitive files up to the root target.
    transitive = depset(
        [out],
        transitive = [
            dep[DetectDepsInfo].transitive_data
            for attr_name in ["deps", "runtime_deps", "data"]
            for dep in getattr(ctx.rule.attr, attr_name, [])
            if DetectDepsInfo in dep
        ],
    )

    return [
        DetectDepsInfo(transitive_data = transitive),
        OutputGroupInfo(dep_info = transitive),
    ]

detect_deps_aspect = aspect(
    implementation = _detect_deps_impl,
    attr_aspects = ["deps", "runtime_deps", "data"],
)
