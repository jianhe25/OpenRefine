#Extended OpenRefine

This project extend OpenRefine to recommend fixing rules to user after user editing one cell. It use very simple algorithm which rank rules by Correlation * #affected_tuples. The core algorithm is 'RecommendationEngine.java'. The core API is 
`recommendChanges()`

### How to run?

Switch to git branch recommend_fix

1. `./refine build`
2. `./refine`
3. open [http://127.0.0.1:3333](http://127.0.0.1:3333)

