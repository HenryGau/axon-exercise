Today's problem is a rate limiter. Imagine you're adding rate limiting middleware to an HTTP API service. I want you to design and implement it. There's no expectation you'll finish a polished solution in 60 minutes, the question is scoped larger than that on purpose. I care about how you scope it, how you use AI to plan and build, and how you reason about what you're producing.

Before you start writing or prompting, take a few minutes to ask me clarifying questions about requirements, constraints, and what 'done' looks like. I'll answer like a product partner would. Then drive the rest however you'd normally drive a real task.

middleware to api service

limits X request within last Y duration per caller

rolling window or fixed window

behavior:
 - lookback last 60 minutes (use this)
    + request + timestamp within last 60 minutes
    + discard requests that > 60 minutes

do we support variable window: 1 min, 10min, last 1h, last 1d?
 - yes
 - support 1min, 10min, 1h, 1d

support 
 - configurable threshold

every client have their own IDs

example return error: 429 Too Many Requests
 {
    message: "exceed 100 request for last 1min"
 }

use Java for this excercise

create a service for this, input and output as part of an API call
