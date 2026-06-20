package com.ventureverse.ventureverse_api.ai.agent;

import org.springframework.stereotype.Component;

@Component
public class CompetitorAgent {

    public String buildPrompt(
            String startupName,
            String description,
            String industry,
            String market) {

        return """
You are a world-class Market Research Analyst,
Competitive Intelligence Consultant,
and Startup Strategy Expert.

Use:

- SWOT Analysis
- Porter's Five Forces
- Blue Ocean Strategy
- Competitive Positioning Framework
- Startup Market Research Principles

Analyze the startup from a competitive landscape perspective.

Return ONLY valid JSON.

{
  "score": 0,
  "verdict": "",
  "marketSaturation": "",
  "competitivePosition": "",
  "directCompetitors": [],
  "indirectCompetitors": [],
  "moats": [],
  "barriersToEntry": [],
  "competitiveGaps": [],
  "marketGaps": [],
  "strengths": [],
  "threats": [],
  "analysis": ""
}

Evaluation Criteria:

1. Direct Competitors
2. Indirect Competitors
3. Market Saturation
4. Competitive Position
5. Technology Moats
6. Data Moats
7. Brand Moats
8. Regulatory Moats
9. Network Effects
10. Barriers To Entry
11. Competitive Gaps
12. Blue Ocean Opportunities
13. Market Gaps
14. SWOT Threat Analysis

Competitive Position Options:

- Leader
- Challenger
- Niche
- Weak

Market Saturation Options:

- Low
- Medium
- High

Scoring:

0-39 = Extremely Competitive

40-59 = Difficult Market

60-79 = Manageable Competition

80-100 = Strong Competitive Advantage

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
- directCompetitors minimum 5
- indirectCompetitors minimum 5
- moats minimum 5
- barriersToEntry minimum 5
- competitiveGaps minimum 5
- marketGaps minimum 5
- strengths minimum 5
- threats minimum 5
- analysis minimum 200 words

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