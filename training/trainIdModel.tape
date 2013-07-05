global {
    ducttape_experimental_imports=true
    ducttape_experimental_packages=true
    ducttape_experimental_multiproc=true

    gcThreads=2
    numThreads=4
    classpath=".:target/Semafor-3.0-alpha-03.jar"
}

package semafor
	 :: .versioner=disk 
	 :: .path="/Users/sam/code/semafor/semafor" 
	 #:: .versioner=git
	 #:: .repo="git@github.com:sammthomson/semafor.git"
	 #:: .ref=HEAD
{
    #mvn package
}

package trainingDir
	:: .versioner=disk
	:: .path="/Users/sam/code/semafor/semafor/training/data/naacl2012" { }

package oldModelDir
	:: .versioner=disk
	:: .path="/Users/sam/code/semafor/models/semafor_malt_model_20121129" { }

task preproc : semafor trainingDir
      > allLemmaTagsFile
      > feFile
      :: dependency_parser=turbo
      :: classpath=@
      :: split=(Split: train dev test)
{
    cd "${semafor}"
    tmp_parse_file="${allLemmaTagsFile}.tmp_parse_file"

    java -classpath ${classpath} -Xms1g -Xmx1g \
        edu.cmu.cs.lti.ark.fn.data.prep.AllAnnotationsMergingWithoutNE \
		$trainingDir/cv.${split}.sentences.tokenized \
		$trainingDir/cv.${split}.sentences.${dependency_parser}parsed.conll \
		${tmp_parse_file} \
		$allLemmaTagsFile
    rm "${tmp_parse_file}"

    cp "${trainingDir}/cv.${split}.sentences.frame.elements" "${feFile}"
}

task newModelDir : oldModelDir
       > dir
{
    mkdir -p "${dir}"
    # link all the needed files from the old model. we don't need to recreate them all
    ln -s ${oldModelDir}/{*.{map,jobj,ser,conf,mco,gz},argmodel.dat} ${dir}/
}

task createAlphabet : semafor oldModelDir
       < allLemmaTagsFile=@preproc[Split:train]
       < feFile=@preproc[Split:train]
       < dir=@newModelDir
       > log
       :: featureType=(Feature: senna basic ancestors)
       :: numThreads=@
       :: gcThreads=@
       :: classpath=@
{

    fe_file_length=`wc -l ${feFile}`
    fe_file_length=`expr ${fe_file_length% *}`
    fn_id_req_data_file="$dir/reqData.jobj"
    cd $semafor
    java -classpath ${classpath} -Xms6g -Xmx6g -XX:ParallelGCThreads=${gcThreads} \
    edu.cmu.cs.lti.ark.fn.identification.training.AlphabetCreationThreaded \
            train-fefile:${feFile} \
            train-parsefile:${allLemmaTagsFile} \
            fnidreqdatafile:${fn_id_req_data_file} \
            logoutputfile:${log} \
            model:${dir} \
            id-feature-extractor-type:${featureType} \
            startindex:0 \
            endindex:${fe_file_length} \
            numthreads:${numThreads}
}

task createIdFeatures : semafor
       < allLemmaTagsFile=@preproc[Split:train]
       < feFile=@preproc[Split:train]
       < dir=@newModelDir
       > log
       > eventDir
       :: featureType=(Feature: senna basic ancestors)
       :: numThreads=@
       :: gcThreads=@
       :: classpath=@
{

    mkdir -p "${eventDir}"
    fe_file_length=`wc -l ${feFile}`
    fe_file_length=`expr ${fe_file_length% *}`
    fn_id_req_data_file="${dir}/reqData.jobj"

    cd $semafor
    java -classpath ${classpath} -Xms8g -Xmx8g -XX:ParallelGCThreads=${gcThreads} \
	edu.cmu.cs.lti.ark.fn.identification.training.ExtractTrainingFeatures \
	    train-fefile:${feFile} \
	    train-parsefile:${allLemmaTagsFile} \
	    fnidreqdatafile:${fn_id_req_data_file} \
	    logoutputfile:${log} \
	    model:${dir}/alphabet.dat \
	    id-feature-extractor-type:${featureType} \
	    eventsfile:${eventDir} \
	    startindex:0 \
	    endindex:${fe_file_length} \
	    numthreads:${numThreads}
}

task trainIdModel : semafor
       < dir=@newModelDir
       < eventDir=@createIdFeatures
       > log
       :: lambda=(Lambda: 1e3 1e-1 1 1e1 1e2 1e4) 
       :: featureType=(Feature: senna basic ancestors)
       :: costMultiple=(CostMultiple: 1 1e-1 5e-1 5 10 50 100)
       :: numThreads=@
       :: gcThreads=@
       :: classpath=@
{
    idModel="${dir}/idmodel.dat"
    alphabetFile="${dir}/alphabet.dat"    
    java -classpath ${classpath} -Xms1g -Xmx8g -XX:ParallelGCThreads=${gcThreads} \
	edu.cmu.cs.lti.ark.fn.identification.training.TrainBatch \
	    alphabetfile:${alphabetFile} \
	    eventsfile:${eventDir} \
	    model:${idModel} \
	    regularization:l1 \
	    lambda:$lambda \
	    restartfile:null \
	    logoutputfile:${log} \
	    numthreads:${numThreads} \
	    use-partial-credit:true \
	    cost-multiple:$costMultiple

    # combine feature names and values
    lastModelFile="$(ls ${idModel}_* | sort -r | head -n1)"
    java -classpath ${classpath} -Xms8g -Xmx8g -XX:ParallelGCThreads=${gcThreads} \
	edu.cmu.cs.lti.ark.fn.identification.training.ConvertAlphabetFile \
	    ${alphabetFile} \
	    ${lastModelFile} \
	    ${idModel} \
	    ${featureType}
}


plan TuneSennaLambda {
     reach trainIdModel via (Feature: senna) * (CostMultiple: 5) * (Lambda: *)
}
