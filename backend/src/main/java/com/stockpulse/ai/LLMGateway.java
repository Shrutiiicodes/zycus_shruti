package com.stockpulse.ai;

/** The only seam the rest of the app depends on. Swap implementations via commerce config, not code changes. */
public interface LLMGateway {
    String call(String prompt);
}