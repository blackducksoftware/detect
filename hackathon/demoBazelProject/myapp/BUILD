# myapp/BUILD — Bazel build targets for the demo application.
#
# AI-ASSIST SIGNAL: BazelAiContextAdapter parses this file and surfaces the
# two targets below as candidate scan targets in Q1:
#
#   BUILD targets found: //myapp:myapp (java_binary), //myapp:myapp_lib (java_library)
#
# The user is expected to choose //myapp:myapp (the production binary) as the
# target to scan.  Selecting //myapp:myapp_lib is also valid but scans fewer deps.

java_library(
    name = "myapp_lib",
    srcs = glob(["src/main/java/com/example/*.java"]),
    deps = [
        "@maven//:com_google_guava_guava",
        "@maven//:com_fasterxml_jackson_core_jackson_databind",
        "@maven//:org_slf4j_slf4j_api",
        "@maven//:ch_qos_logback_logback_classic",
    ],
    visibility = ["//visibility:public"],
)

java_binary(
    name = "myapp",
    main_class = "com.example.App",
    runtime_deps = [":myapp_lib"],
    visibility = ["//visibility:public"],
)


