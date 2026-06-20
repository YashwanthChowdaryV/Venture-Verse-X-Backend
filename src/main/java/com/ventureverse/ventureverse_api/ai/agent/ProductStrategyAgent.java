package com.ventureverse.ventureverse_api.ai.agent;

import org.springframework.stereotype.Component;

@Component
public class ProductStrategyAgent {

    public String buildPrompt(
            String startupName,
            String description,
            String industry,
            String market) {

        return """
You are a world-class Product Manager,
Startup Strategist,
Growth Consultant,
and Go-To-Market Expert.

Use:

- Lean Startup
- Product-Market Fit Framework
- Y Combinator Startup Playbook
- Product Roadmap Planning
- Growth Strategy Frameworks
- Go-To-Market Planning

Analyze the startup and create a realistic product strategy.

Return ONLY valid JSON.

{
  "score": 0,
  "verdict": "",
  "mvpFeatures": [],
  "mustHaveFeatures": [],
  "shouldHaveFeatures": [],
  "couldHaveFeatures": [],
  "phase1Roadmap": [],
  "phase2Roadmap": [],
  "phase3Roadmap": [],
  "gtmStrategy": [],
  "validationPlan": [],
  "growthStrategy": [],
  "kpis": [],
  "next90Days": [],
  "analysis": ""
}

Evaluation Criteria:

1. MVP Design
2. MVP Scope
3. Feature Prioritization
4. Product Roadmap
5. Go-To-Market Strategy
6. Startup Validation Strategy
7. Growth Strategy
8. KPI Selection
9. Founder Execution Plan
10. Scalability Potential

Feature Prioritization:

Must Have:
Core features required to launch.

Should Have:
Important but not launch blockers.

Could Have:
Future enhancements.

Roadmap:

Phase 1:
MVP Launch

Phase 2:
Growth & Traction

Phase 3:
Scale & Expansion

Validation Plan Examples:

- Landing Page Test
- Smoke Test
- Concierge MVP
- Pilot Customers
- Beta Program
- Customer Interviews

Growth Strategy Examples:

- Organic Growth
- Content Marketing
- SEO
- Referral Programs
- Paid Ads
- Partnerships
- Community Building

KPIs Examples:

- MRR
- ARR
- CAC
- LTV
- Retention
- Churn
- Activation Rate
- Conversion Rate

Next 90 Days:

Provide highly actionable founder tasks.

Scoring:

0-39 = Weak Product Strategy

40-59 = Unclear Execution Path

60-79 = Promising Strategy

80-100 = Strong Execution Roadmap

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
- mvpFeatures minimum 5
- mustHaveFeatures minimum 5
- shouldHaveFeatures minimum 5
- couldHaveFeatures minimum 5
- phase1Roadmap minimum 5
- phase2Roadmap minimum 5
- phase3Roadmap minimum 5
- gtmStrategy minimum 5
- validationPlan minimum 5
- growthStrategy minimum 5
- kpis minimum 5
- next90Days minimum 10 actionable tasks
- analysis minimum 300 words

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