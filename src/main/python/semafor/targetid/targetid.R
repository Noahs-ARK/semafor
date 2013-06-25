set.seed(1010)

#data <- read.table("/Users/sam/repo/project/semafor/semafor/training/data/results/pos_targ_nt.csv", sep="\t", header=TRUE)
#data <- read.table("/Users/sam/repo/project/semafor/semafor/training/data/results/pos_targ_nt.csv", sep="\t", header=TRUE)
#data$target_percent = data$target / (data$target + data$nt)
#data$nt_percent = data$nt / (data$target + data$nt)
#top <- data[with(data, order(-target_percent)),]
#barplot(t(as.matrix(data.frame(target=top$target_percent, nt=top$nt_percent))), names.arg=top$pos)

library(randomForest)

data = read.table("/Users/sam/repo/project/semafor/semafor/training/data/naacl2012/cv.train.targetid.features.tsv", sep="\t", header=TRUE, quote='"', comment.char="")
d <- ncol(data)
n <- 1000 # nrow(data)
#data = read.table("/Users/sam/repo/project/semafor/semafor/training/data/naacl2012/cv.train.targetid.features.head.tsv", sep="\t", header=TRUE, quote='"', comment.char="")
data[is.na(data)] = 0
data <- as.data.frame(lapply(data, detect.and.convert.factors))
split <- 2 * n / 3
data.train = data[1:split,]
data.test = data[split:n,]

rffit = randomForest(data.train[,2:d], data.train$is_target)
rfpred = predict(rffit, data.test[,2:d], type="response")


library(glmnet)
lrfit = cv.glmnet(data.matrix(data.train[,2:d]), data.train$is_target, family="binomial") # LASSO (L1)

tpred = predict(lrfit, data.matrix(data.test[,2:d]), type="response", s="lambda.min")
mte = mean(((tpred > .5)-(data.test$is_target=="True"))^2)
