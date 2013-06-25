set.seed(1015)

library(glmnet)
library(Matrix)
library(multicore)
library(caret)

setwd("/home/sthomson/code/semafor/semafor")
data.train.triples <- read.csv("training/data/naacl2012/frameid/train.tsv", sep="\t")
data.dev.triples <- read.csv("training/data/naacl2012/frameid/dev.tsv", sep="\t")
data.test.triples <- read.csv("training/data/naacl2012/frameid/test.tsv", sep="\t")
data.all <- rbind(
    data.train.triples,
    data.dev.triples,
    data.test.triples
)

# separate out the output
getFrames <- function(data) {
    return(data[which(data$feature_name=="frame"),]$value)
}
Y.train.all <- factor(getFrames(data.train.triples)) #, levels=levels(frames.all))

# throw out frames that appear 2 or less times (account for 1.55% of training data)
frameCounts <- sapply(levels(Y.train.all), function(l) sum(Y.train.all == l))
#rareFrames <- frameCounts[which(frameCounts <= 5)]
commonFrames <- names(which(frameCounts > 2))
targetIdsOfCommonFrames <- data.train.triples[which(data.train.triples$value %in% commonFrames),]$target_id
data.train.triples.common <- data.train.triples[which(data.train.triples$target_id %in% targetIdsOfCommonFrames),]

Y.train <- factor(getFrames(data.train.triples.common))
Y.dev <- factor(getFrames(data.dev.triples), levels=levels(Y.train)) #frames.all))
Y.test <- factor(getFrames(data.test.triples), levels=levels(Y.train)) #frames.all))


#frames.all <- factor(getFrames(data.all))
#data.all <- data.all[which(data.all$feature_name != "frame"),]
feature_names <- factor(data.all[which(data.all$feature_name != "frame"),2])
num.features <- length(levels(feature_names))

# target_ids become 1-indexed
# feature_names are now indexed by their level in the factor
toSparse <- function(data) {
    noFrames <- data[which(data$feature_name != "frame"),]
    return(
        sparseMatrix(
            i=as.integer(factor(noFrames$target_id)),
            j=as.integer(factor(noFrames$feature_name, levels=levels(feature_names))),
            x=as.double(as.character(noFrames$value)),
            dims=c(length(levels(factor(noFrames$target_id))), num.features)) #index1=FALSE)
    )
}



X.train <- toSparse(data.train.triples.common)
X.dev <- toSparse(data.dev.triples)
X.test <- toSparse(data.test.triples)

#MultiControl <- trainControl(
#    method="cv",
#    number=3
#)
#MultiControl$workers <- 16 #multicore:::detectCores(),
#MultiControl$computeFunction <- mclapply

lambdas <- 10 ^ seq(3, -3, by=-1)
lr.fit <- cv.glmnet(X.train, Y.train, family="multinomial", lambda=lambdas, nfolds=3, standardize=FALSE)
#model <- train(
#    X.train,
#    Y.train,
#    method='glmnet',
#    tuneGrid = expand.grid(.alpha=seq(0,1,by=.25),.lambda=seq(0,.25,by=0.005)),
#    trControl=MultiControl
#)


#to save
# writeMM(obj, file)




library(randomForest)
rf.fit = randomForest(X.train, Y.train, ntree=200, do.trace=TRUE, keep.forest=TRUE)
