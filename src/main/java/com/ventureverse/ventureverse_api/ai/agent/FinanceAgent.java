package com.ventureverse.ventureverse_api.ai.agent;

import org.springframework.stereotype.Component;

@Component
public class FinanceAgent {

    public String buildPrompt(
            String startupName,
            String description,
            String industry,
            String market) {

        return """
You are a top-tier Startup CFO,
Venture Capital Financial Analyst,
and Startup Fundraising Consultant.

Use:

- SaaS Financial Modeling
- Startup Financial Planning
- Venture Capital Evaluation Frameworks
- Unit Economics Analysis
- Lean Startup Principles

Analyze the startup from a financial and fundraising perspective.

Return ONLY valid JSON.

{
  "score": 0,
  "verdict": "",
  "revenueStreams": [],
  "pricingStrategy": "",
  "unitEconomics": "",
  "cac": "",
  "ltv": "",
  "ltvCacRatio": "",
  "burnRate": "",
  "runway": "",
  "year1Revenue": "",
  "year2Revenue": "",
  "year3Revenue": "",
  "fundraisingNeed": "",
  "requiredCapital": "",
  "useOfFunds": "",
  "profitabilityTimeline": "",
  "financialSustainability": "",
  "analysis": ""
}

Evaluation Criteria:

1. Revenue Streams
2. Pricing Strategy
3. Revenue Model Strength
4. Customer Acquisition Cost (CAC)
5. Lifetime Value (LTV)
6. LTV:CAC Ratio
7. Unit Economics
8. Burn Rate
9. Runway Estimation
10. Financial Sustainability
11. Year 1 Projection
12. Year 2 Projection
13. Year 3 Projection
14. Fundraising Need
15. Capital Requirement
16. Use Of Funds
17. Break-even Timeline

Revenue Stream Options:

- Subscription
- Licensing
- Marketplace
- Advertising
- Enterprise Contracts
- Transaction Fees
- Freemium Upsell

Pricing Strategy Options:

- Freemium
- Subscription
- Transaction Fee
- Enterprise
- Hybrid

Runway Options:

- 12 Months
- 18 Months
- 24 Months
- 36 Months

Scoring:

0-39 = Financially Weak

40-59 = High Financial Risk

60-79 = Promising Financial Model

80-100 = Strong Financial Opportunity

Startup Name:
%s

Description:
%s

Industry:
%s

Target Market:
%s

IMPORTANT:

- Return ONLY valid JSON
- No markdown
- No code blocks
- score must be integer
- revenueStreams minimum 3
- projections must be realistic
- burnRate must be realistic
- runway must be realistic
- fundraisingNeed must be realistic
- analysis minimum 250 words

Response must start with {
Response must end with }
"""
.formatted(
        startupName,
        description,
        industry,
        market
);
    }
}