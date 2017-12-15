*Performance problem*

There are ways to improve the current algorithm:
- select targets better (don't include already surrounded cells)
- stop filling as soon as any direction touches a wall

Need to check if applied that the surrounded status and walls marking mechanisms still work as before. This requires adding a more complex tests best on UI experiments.
UI experiments require additional UI hints (colors or added symbols). We need to see which cells are walls and which are surrounded. Then it should be easier to create test data manually by simply playing the game.

Then I can probably run _time_ function against smaller pieces to find out where the slowness is.

Also, need to check the performance of react by using some browser tool. I have a suspicion that the engine re-renders the whole board instead of a small dot. It's probably easy to check if I decrease the board size - if the time decreases significantly - then it's that. But I guess debug tools will let me know how many renders we're doing after clicking on one dot.

*Drawing walls*

After playing a little bit I've noticed that the walls are drawn in unpredictable ways as soon as game state becomes more than super simple (several components in different parts of the screen). I need to research some algorithms. The problem was probably solved before.

To start with there are:
- spanning trees (but how do I make them cyclical?)
- different graph cycles.

A good idea would be to look through the red book of algorithms.

After looking through the book I've came across the classic convex hull algorithm and decided that it's a good fit for drawing walls. I just need to keep walls clustered together by groups and then run the algorithm on each cluster of walls. The convex hull will make sure I always draw an outline of the cluster.

At first I didn't limit the convex hull algorithm and just implemented the simplest solution (start with left most and then select all the most counterclockwise points). However the no-limit meant that the hull can skip one or more dots if a cluster has a better candidate even if it's far away. So the picture looked like some weird connect the dots game with skipped dots. When I changed the algorithm to check only the 1-step away points as candidates the image improved. But...

The new issue at the moment though is neighboring clusters. It's those clusters of walls that are one step apart from each other. Single point of connection works fine - I've checked. But when the clusters have two points as direct neighbors - it looks like I get an eternal loop (not sure how yet). So debugging is needed.
