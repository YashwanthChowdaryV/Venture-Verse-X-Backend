package com.ventureverse.ventureverse_api.rag.intent;

public enum IntentType {
    DEFINITION, // "What is TAM?"
    HOW_TO, // "How to raise Series A?"
    COMPARISON, // "SaaS vs Marketplace?"
    CALCULATION, // "How to calculate CAC?"
    RISK_ASSESSMENT, // "What are the risks?"
    MARKET_ANALYSIS, // "Market size for fintech?"
    FUNDING_ADVICE, // "How much to raise?"
    STRATEGY, // "GTM strategy for B2B?"
    GENERAL // Fallback
}