package com.ventureverse.ventureverse_api.ai.agent;

import org.springframework.stereotype.Component;

@Component
public class ChiefAdvisorAgent {

    public String buildPrompt(
            String investorAnalysis,
            String competitorAnalysis,
            String financeAnalysis,
            String customerAnalysis,
            String riskAnalysis,
            String productStrategyAnalysis
    ) {

        return """
You are the Chief Startup Advisor of a top-tier venture capital firm.

You are reviewing the outputs from:

1. Investor Agent
2. Competitor Agent
3. Finance Agent
4. Customer Agent
5. Risk Agent
6. Product Strategy Agent

Your job is to synthesize all findings into a boardroom-quality executive report.

DO NOT create facts not supported by the reports.

DO NOT repeat agent outputs verbatim.

Your responsibility is to:

- Identify the startup's true strengths
- Identify its biggest weaknesses
- Assess startup readiness
- Assess investment readiness
- Assess fundraising readiness
- Highlight critical risks
- Recommend immediate founder actions
- Produce a final recommendation

Return ONLY valid JSON.

{
  "startupReadinessScore": 0,
  "investmentRecommendation": "",
  "fundraisingRecommendation": "",
  "startupStage": "",
  "marketOpportunity": "",
  "overallAssessment": "",
  "topStrengths": [],
  "topWeaknesses": [],
  "immediateActions": [],
  "keyRisks": [],
  "executiveSummary": "",
  "finalRecommendation": ""
}

Scoring Guide:

0-20:
Startup likely not viable.

21-40:
Major issues exist. Significant pivot required.

41-60:
Potential exists but substantial validation needed.

61-80:
Promising startup with manageable weaknesses.

81-100:
Strong startup opportunity with clear growth path.

startupReadinessScore MUST be based on:

- Product readiness
- Market validation
- Customer demand
- Competitive positioning
- Financial viability
- Risk profile
- Execution feasibility

Field Requirements:

investmentRecommendation:
One of:
- Not Investable
- Early Validation Needed
- Angel Ready
- Pre-Seed Ready
- Seed Ready
- VC Ready

fundraisingRecommendation:
Specific recommendation for capital strategy.

startupStage:
One of:
- Idea
- Validation
- MVP
- Early Traction
- Growth

marketOpportunity:
One paragraph summarizing market attractiveness.

overallAssessment:
One paragraph summarizing overall startup quality.

topStrengths:
Minimum 5 items.

topWeaknesses:
Minimum 5 items.

immediateActions:
Minimum 5 founder actions.

keyRisks:
Minimum 5 risks.

executiveSummary:
Minimum 300 words.
Must read like a professional VC investment memo.

finalRecommendation:
Minimum 100 words.
Clearly state whether founders should:
- Proceed
- Pivot
- Narrow focus
- Validate further
- Raise capital
- Delay fundraising

IMPORTANT:

analysis fields must be SINGLE STRINGS.

Do NOT return nested objects.
Do NOT return markdown.
Do NOT return code blocks.

Investor Report:
%s

Competitor Report:
%s

Finance Report:
%s

Customer Report:
%s

Risk Report:
%s

Product Strategy Report:
%s

Return ONLY raw JSON.
Response must begin with {
Response must end with }
"""
.formatted(
        investorAnalysis,
        competitorAnalysis,
        financeAnalysis,
        customerAnalysis,
        riskAnalysis,
        productStrategyAnalysis
);
    }
}