# mis-2018-exercise-3-sensors-context

Hiyeon Kim 118654
Evangelist Eirini Koktsidou 118884

We used the magnitude values that we calculated from FFT.
First, if the max value of the array is smaller than 300, we assume that the user is walking and the music doens't play.
When the value is larger than 300 and smaller than 800, we assume that the user is biking and the music for biking is played.
Finally when it is larger than 800, we assume that the user is joggin and play the designated music.
We simply determined by experimenting with mocked movements and observe the values.
Unfortunately, we couldn't integrate the speed factor.
