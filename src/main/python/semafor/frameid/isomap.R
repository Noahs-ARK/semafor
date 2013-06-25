set.seed(1015)
setwd("/home/sthomson/code/semafor/semafor")


library(vegan)
library(kernlab)

dist <- read.csv("src/main/resources/frame_distances.csv")


i <- isomap(dist, ndim=2, k=10)
h <- i$points


png(file="frames_isomap.png", width=4000, height=4000, res=400)
plot(x=h[,1], y=h[,2], pch = '', xlab="Component 1", ylab="Component 2")
text(x=h[,1], y=h[,2], labels=dimnames(h)[[1]], cex=0.2)
dev.off()


