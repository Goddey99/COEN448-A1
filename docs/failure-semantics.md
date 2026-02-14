# Failure Semantics in Concurrent Systems

## Overview

In concurrent systems, multiple microservices execute in parallel and may complete in nondeterministic order. The key design question is how the system behaves when one or more services fail. This document explains three failure-handling policies: Fail-Fast, Fail-Partial, and Fail-Soft, and the trade-offs they introduce.

1. Fail-Fast (Atomic Policy)

Behavior:
If any microservice fails, the entire operation fails. No partial result is returned, and the exception propagates to the caller.

When to Use:
When correctness and consistency are critical and partial results are unacceptable.

Example:
In a payment transaction, if fraud validation fails, the transaction must be aborted even if other services succeed.

Trade-off:
Maximizes correctness but reduces availability.

2. Fail-Partial (Best-Effort Policy)

Behavior:
Successful services return results, while failed services are ignored. The overall computation completes normally.

When to Use:
When partial results are still useful, such as dashboards or aggregation systems.

Example:
A news aggregator displays articles from available sources even if some sources fail.

Trade-off:
Improves availability but may hide degraded system behavior if failures are not monitored.

3. Fail-Soft (Fallback Policy)

Behavior:
Failures are replaced with predefined fallback values. The computation never fails.

When to Use:
When uninterrupted service is more important than perfect accuracy.

Example:
A recommendation system shows default recommendations if personalization fails.

Trade-off:
Maximizes availability but can mask serious errors if logging and monitoring are insufficient.

Risks of Hiding Failures

Fail-Partial and Fail-Soft policies can conceal underlying system instability. Without proper monitoring, failures may remain undetected, leading to silent degradation.

Conclusion

Failure semantics must be explicitly defined in concurrent systems. The appropriate policy depends on whether the system prioritizes correctness (Fail-Fast), partial progress (Fail-Partial), or availability (Fail-Soft). Clear documentation of these trade-offs is essential for reliable system design.
