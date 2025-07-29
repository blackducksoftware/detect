# [detect_product_short] usage metrics collection

[detect_product_long] uses Google Analytics to collect anonymized usage metrics through a mechanism called *phone home*.
[var_company_name] uses this data to help set engineering priorities.

In a network where access to outside servers is limited, this mechanism may fail, and those failures
may be visible in the log. These are harmless failures; [detect_product_short] will continue to function
normally.

To disable this mechanism for [detect_product_short] runs executed from one environment,
set the environment variable *BLACKDUCK_SKIP_PHONE_HOME* to *true*.
To disable this mechanism for all [detect_product_short] runs against a specific [bd_product_short]
server, refer to the [bd_product_short] documentation for information on disabling analytics.