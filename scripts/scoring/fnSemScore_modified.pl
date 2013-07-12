#!/usr/bin/env perl

# ########################################################################
#
# fssScore.pl - Frame Semantic Structure Extraction Scorer
#
# ########################################################################

use strict;
use Data::Dumper;
use File::Basename;
use Getopt::Std;
use Storable;
use XML::Parser;
use vars qw($opt_c $opt_d $opt_e $opt_h $opt_l $opt_n $opt_s $opt_t $opt_v);

# global variables
my $PROG = basename($0);
my $DEBUG = 0;
my $VERBOSE = 0;
my $OUTPUT_PER_SENTENCE = 0;
my $FRAME_POINTS = 1;
my $COREFE_POINTS = 1;
my $NONCOREFE_POINTS = .5;
my $NE_POINTS = .5;
my $SUPP_POINTS = .5;
my $SEPARATOR = ';';
my %CORELOOKUP;
my $PREC = 5;       # number of decimal digits in Precision, Recall, Fscore

# FLAG for input XML format: 0=SEM XML, 1=FN FT XML 
my $FRAMENETXML = 0;

# global variables used for printing matches
my $MISSED = 0;
my $MATCHED = 1;
my $EXTRA = 2;
my $FEATURE_WIDTH = 80;
my $SCORE_WIDTH = 15;

# recursion limit for BF SP finder
my $MAX_DISTANCE = 9;
my $FRSPLENLOOKUP;
my $FRSPNUMLOOKUP;
my $FESPLENLOOKUP;
my $FESPNUMLOOKUP;
my $NOPARTIALCREDIT = 0;

# label mismatch types
my $EXTRA_LABEL = 1;
my $MISSING_LABEL = 2;

# fraction of points retained per traversal of relation
my $LINK_WEIGHT = 0.8;

# directory where cache is created
my $CACHEDIR = "/tmp";

my $CMDOPTIONS;

MAIN:{
    
    #################################################################
    # PROCESS OPTIONS

    getopts("c:d:ehlnstv") || &usage();
    if (defined $opt_c) {
        $CACHEDIR = $opt_c;
        if (! -e $CACHEDIR) {die "ERROR: $CACHEDIR doesn't exist!\n"};
        if (! -d $CACHEDIR) {die "ERROR: $CACHEDIR isn't a directory!\n"};
        $CMDOPTIONS .= "-c $CACHEDIR ";
    }
    if (defined $opt_d) {
	$DEBUG = $opt_d;
        $CMDOPTIONS .= "-d $DEBUG";
    }
    if (defined $opt_e) {
        $NOPARTIALCREDIT = 1;
        $CMDOPTIONS .= "-e ";
    }
    if (defined $opt_l) {
        $FRAMENETXML = 1;
        $CMDOPTIONS .= "-l ";
    }
    if (defined $opt_n) {
        $NE_POINTS = 0;
        $CMDOPTIONS .= "-n ";
    }
    if (defined $opt_s) {
        $OUTPUT_PER_SENTENCE = 1;
        $CMDOPTIONS .= "-s ";
    }
    if (defined $opt_t) {
        $COREFE_POINTS = 0;
        $NONCOREFE_POINTS = 0;
        $NE_POINTS = 0;
        $SUPP_POINTS = 0;
        $CMDOPTIONS .= "-t ";
    }
    if (defined $opt_v) {
        $VERBOSE = 1;
        $CMDOPTIONS .= "-v ";
    }
    # read arguments
    if ($#ARGV!=3 || defined($opt_h)) {
	&usage();
    }
    my $framesFile = shift(@ARGV);
    my $frRelsFile = shift(@ARGV);
    my $f1 = shift(@ARGV);
    my $f2 = shift(@ARGV);

    my $f;
    foreach $f ($framesFile, $f1, $f2) {
        if (! -f $f) {
            die ("ERROR: could not read $f\n");
        }
    }

    #
    #####################################################################

    ##################################
    # READ IN XML FILES
    
    # populate global CORELOOKUP (core vs. non-core)
    %CORELOOKUP = &CreateFELookup($framesFile);

    # read-in frRelation lookups
    ($FRSPLENLOOKUP,$FRSPNUMLOOKUP,$FESPLENLOOKUP,$FESPNUMLOOKUP) = &ParseProcessFrRelXML($frRelsFile);

    # read gold standard XML
    my $goldStandFr;
    my $goldStandFe;
    my $goldText;
    my $goldWordLookup;

    print "Parsing Gold Standard: $f1\n" if $VERBOSE;
    if ($FRAMENETXML) {
        ($goldStandFr, $goldStandFe, $goldText, $goldWordLookup) = &ParseFNXMLFile($f1);
    } else {
        ($goldStandFr, $goldStandFe, $goldText, $goldWordLookup) = &ParseSemXMLFile($f1);
    }

    # read the xml to score
    my $toScoreFr;
    my $toScoreFe;
    my $toScoreText;
    my $toScoreWordLookup;
    
    print "Parsing Input to Score: $f2\n" if $VERBOSE;
    if ($FRAMENETXML) {
        ($toScoreFr, $toScoreFe, $toScoreText, $toScoreWordLookup) = &ParseFNXMLFile($f2);
    } else {
        ($toScoreFr, $toScoreFe, $toScoreText, $toScoreWordLookup) = &ParseSemXMLFile($f2);
    }
 
    ##########################################
    # COMPARISON AND SCORE CALCULATION

    my $sent;
    my $totalMatchSum = 0;
    my $totalScoreSum = 0;
    my $totalGoldSum = 0;
    my $nSent = 0;

    foreach $sent (sort{$a<=>$b} keys %{$goldStandFr}) {
	my (%goldFrFeats, %scoreFrFeats);
	my (%goldFeFeats, %scoreFeFeats);
        my (%goldWL, %scoreWL);
	my $goldSum = 0;
        my $scoreSum = 0;

	# check if sentences are missing, 0 points
	if (!defined($toScoreText->{$sent})) {
	    print STDERR "WARNING: sentence $sent is missing in $f2.  Skipping\n";
	    next;
	}

	# calculate gold standard point total
	
        if (defined $goldStandFr->{$sent}) {
            %goldFrFeats = %{$goldStandFr->{$sent}};
        }
	if (defined $goldStandFe->{$sent}) {
            %goldFeFeats = %{$goldStandFe->{$sent}};
        }
	$goldSum = &sumFrFeatValues(\%goldFrFeats);
	$goldSum += &sumFeFeatValues(\%goldFeFeats);
	if ($goldSum==0) {
	    print STDERR "No gold standard annotation on sentence $sent. Skipping.\n";
	    next;
	}

	# comparison is valid only if the text is identical
	if ($goldText->{$sent} ne $toScoreText->{$sent}) {
	    print STDERR "WARNING: text of sentence $sent differ in content. Skipping.\n";
            print STDERR "        GS: ".$goldText->{$sent}."\n";
            print STDERR "     Input: ".$toScoreText->{$sent}."\n";
	    next;
	}

	print "sentences with ID $sent have identical text\n" if $DEBUG;
	if (defined $toScoreFr->{$sent}) {
            %scoreFrFeats = %{$toScoreFr->{$sent}};
        }
        if (defined $toScoreFe->{$sent}) {
            %scoreFeFeats = %{$toScoreFe->{$sent}};
        }
	$scoreSum = &sumFrFeatValues(\%scoreFrFeats);
	$scoreSum += &sumFeFeatValues(\%scoreFeFeats);
	
	print "goldSum = $goldSum\n" if $DEBUG;
	print "scoreSum = $scoreSum\n" if $DEBUG;

	# Calculate Precision and Recall and Fscore

	# Sum up points for gold standard features that are matched
	my $feature;
	my $matchSum = 0;

        # Column Labels for VERBOSE mode
        print(("-" x $FEATURE_WIDTH)."\n") if $VERBOSE;
        print "Sent# $sent:\n" if $VERBOSE;
        &printSentence($sent,$goldText,$goldWordLookup) if $VERBOSE;

        &printMatch("ColumnLabels",1,1,1) if $VERBOSE;

        # check for FRAME feature matches
        foreach $feature (sort keys %goldFrFeats) {

            if (defined $scoreFrFeats{$feature}) {
		# FR/SU/NE BOUNDARY MATCH! -- need to check labels and FEs...
		
		my $featureType;
		if ($feature =~ /=FR$/) {
		    $featureType = "FR";
		} else {
		    $featureType = "NESU";
		}

		my ($frPairs, $frExtraG, $frExtraS) = &FindPairs($featureType, $goldFrFeats{$feature}, $scoreFrFeats{$feature},$FRSPLENLOOKUP);
		
		# LOOP though matches
		my $frPair;
		foreach $frPair (@{$frPairs}) {

		    my ($gfr, $sfr) = @{$frPair};

		    my $gfeat = $feature.'='.$gfr;
		    my $sfeat = $feature.'='.$sfr;
		    my $gscore = $goldFrFeats{$feature}->{$gfr};
		    my $sscore = $scoreFrFeats{$feature}->{$sfr};
		    my $score = $sscore;
		
		    # FRAME TARGETs may get partial credit
		    my $frDist = 0;
		    my $frPaths = 1;
		    if ($gfr ne $sfr) {
			$frDist = $FRSPLENLOOKUP->{$gfr}->{$sfr};
			$frPaths = $FRSPNUMLOOKUP->{$gfr}->{$sfr};
			$score = $scoreFrFeats{$feature}->{$sfr} * ($LINK_WEIGHT ** $frDist);
		    }
		    
		    printMatch("matching: $feature $sfr",$score,$sscore,$gscore) if $VERBOSE;
		    if ($frDist > 0) {
			printMatch("   $frDist hops to GS: $feature $gfr",0,0,0) if $VERBOSE;
                        print("   Path: ".join(" ->\n            ",&GetSP($sfr,$gfr,$FRSPLENLOOKUP))."\n") if $VERBOSE;
		    }
		    $matchSum += $score;

		    # NE & SUPPs don't have FEs to score
		    if ($featureType eq "NESU") {
			next;
		    }

		    my $feSpan;

		    #Check if there are GS FE labels to score
		    if (!defined($goldFeFeats{$gfeat})) {
			# no FE features on gold standard
			# check if there are FE features on toScore
                        &PrintNoMatchFEs($EXTRA_LABEL,\%scoreFeFeats,$sfeat,$sent) if $VERBOSE;
			next; #skip to next FR
		    }

                    #Check if there are FE labels to score in score data
                    if (!defined($scoreFeFeats{$sfeat})) {
                        # no FE features on score data
                        &PrintNoMatchFEs($MISSING_LABEL,\%goldFeFeats,$gfeat,$sent) if $VERBOSE;
                        next;
                    }

		    # extract hashes: FESpan -> FELabel
		    my %goldFEs = %{$goldFeFeats{$gfeat}};
		    my %scoreFEs = %{$scoreFeFeats{$sfeat}};

		    foreach $feSpan (sort keys %goldFEs) {

			if (defined $scoreFEs{$feSpan}) {
			    # there is a boundary match

                            my ($pairs, $extraG, $extraS) = &FindPairs("FE",$goldFEs{$feSpan},$scoreFEs{$feSpan},$FESPLENLOOKUP);

                            my $pair;
                            # LOOP THROUGH MATCHES
			    foreach $pair (@{$pairs}) {
                                my ($gfe, $sfe) = @{$pair};
                                
				# FE label match
				my $feDist = 0;
				my $fePaths = 1;
				my $factor = 1;
                                my $gscore = $goldFEs{$feSpan}->{$gfe};
				my $sscore = $scoreFEs{$feSpan}->{$sfe};
				my $score = $sscore;

				if ($gfe ne $sfe) {
				    $feDist = $FESPLENLOOKUP->{$gfe}->{$sfe};
				    $fePaths = $FESPNUMLOOKUP->{$gfe}->{$sfe};
				    $factor = $fePaths / $frPaths;
                                    # this is a safety limit, Error is reported later
				    if ($factor > 1) {
                                        $factor = 1;
				    }
				    $score = ($scoreFEs{$feSpan}->{$sfe} * ($LINK_WEIGHT ** $feDist)) * $factor;
				}

                                if ($frDist != $feDist) {
                                    # this means that none of the shortest frame relation paths
                                    # has this FE relation: NO POINTS
                                    printMatch("  no match: $sfeat $feSpan $sfe",0,$sscore,0) if $VERBOSE;
                                    printMatch("     to GS: $gfeat $feSpan $gfe",0,0,$gscore) if $VERBOSE;
                                    next;
                                }
                                
                                if ($factor > 1) {
                                    print STDERR "ERROR: sent $sent: comparing GS $gfeat to $sfeat on $feSpan\n".
                                        "        The number of shortest FE paths from \n".
                                        "        $gfe to $sfe ($fePaths) \n".
                                        "        is larger than the number of shortest Frame paths from\n".
                                        "        $gfr to $sfr ($frPaths).\n".
                                        "        Shortest path has length $feDist.\n".
                                        "        FE Path: ".join(" ->\n                    ",&GetSP($sfe,$gfe,$FESPLENLOOKUP))."\n".
                                        "        Frame Path: ".join(" ->\n                        ",&GetSP($sfr,$gfr,$FRSPLENLOOKUP))."\n".
                                        "        Multiplier was capped at 1.\n";                                    
                                }
                                
                                # match!
				printMatch("  matching: $sfeat $feSpan $sfe",$score,$sscore,$gscore) if $VERBOSE;
				if ($feDist > 0) {
				    printMatch("     $feDist hops to GS: $gfeat $feSpan $gfe",0,0,0) if $VERBOSE;
                                    print("     Path: ".join(" ->\n              ",&GetSP($sfe,$gfe,$FESPLENLOOKUP))."\n") if $VERBOSE;
				}
				if ($factor < 1) {
				    printMatch("     fraction of paths: $fePaths / $frPaths",0,0,0) if $VERBOSE;
				}
				$matchSum += $score;
			    }
                            
                            my $gfe;
                            foreach $gfe (@{$extraG}) {
                                my $gscore = $goldFEs{$feSpan}->{$gfe};
                                printMatch("  missing: $gfeat $feSpan $gfe",0,0,$gscore) if $VERBOSE;
                            }
                            
                            my $sfe;
                            foreach $sfe (@{$extraS}) {
                                my $sscore = $scoreFEs{$feSpan}->{$sfe};
                                printMatch("  extra: $sfeat $feSpan $sfe",0,$sscore,0) if $VERBOSE;
                            }
			    
			} else {
			    # FE boundaries didn't match
                            my $gfe;
                            foreach $gfe (sort keys %{$goldFEs{$feSpan}}) {
                                my $gscore = $goldFEs{$feSpan}->{$gfe};
                                printMatch("  missing: $gfeat $feSpan $gfe",0,0,$gscore) if $VERBOSE;
                            }
			}
		    }
		    # At this point, loop through scoreFEs and look for boundary mismatches
		    foreach $feSpan (sort keys %scoreFEs) {    
			my @sFE = keys %{$scoreFEs{$feSpan}};
                        my $sfe;
                        foreach $sfe (@sFE) {
                            my $sscore = $scoreFEs{$feSpan}->{$sfe};
                            if (!defined $goldFEs{$feSpan}) {
                                printMatch("  extra: $sfeat $feSpan $sfe",0,$sscore,0) if $VERBOSE;
                            }		
                        }
		    }
		}

		my $gfr;
		foreach $gfr (@{$frExtraG}) {
		    my $gscore = $goldFrFeats{$feature}->{$gfr};
		    printMatch("missing: $feature $gfr",0,0,$gscore) if $VERBOSE;
		    if ($feature !~ /=FR$/) {
			next;
		    }
		    my $gfeat = $feature.'='.$gfr;
                    &PrintNoMatchFEs($MISSING_LABEL,\%goldFeFeats,$gfeat,$sent) if $VERBOSE;
		}
		
		my $sfr;
		foreach $sfr (@{$frExtraS}) {
		    my $sscore = $scoreFrFeats{$feature}->{$sfr};
		    printMatch("extra: $feature $sfr",0,$sscore,0) if $VERBOSE;
		    if ($feature !~ /=FR$/) {
			next;
		    }
		    my $sfeat = $feature.'='.$sfr;
                    &PrintNoMatchFEs($EXTRA_LABEL,\%scoreFeFeats,$sfeat,$sent) if $VERBOSE;
		}
		
	    } else {
		# FRAME BOUNDARIES did not match
		
                my $gfr;
                foreach $gfr (keys %{$goldFrFeats{$feature}}) {
                    my $gscore = $goldFrFeats{$feature}->{$gfr};

                    # Award Points for identifying NE WEA for Frame Weapon
                    if ($feature =~ /=FR$/ && $gfr eq "Weapon") {
                        my $nefeat = $feature;
                        $nefeat =~ s/=FR$/=NE/;
                        if (defined $scoreFrFeats{$nefeat} && defined $scoreFrFeats{$nefeat}->{WEA}) {
                            my $score = $scoreFrFeats{$nefeat}->{WEA};
                            # then give partial credit
                            printMatch("exceptional match: $nefeat WEA with GS $feature $gfr",$score,0,0) if $VERBOSE;
                            
                            $matchSum += $score;
                        }
                    }
                    
                    print "missing frame: $feature $gfr ($gscore pts)\n" if $DEBUG;
                    printMatch("missing: $feature $gfr",0,0,$gscore) if $VERBOSE;
                    
                    # NE/SUPPs don't have FEs
                    if ($feature =~ /=(NE|SU)$/) {
                        next;
                    }
                
                    my $gfeat = $feature.'='.$gfr;
                    
                    # if the frame label doesn't match
                    # all its FEs also will not match
                    &PrintNoMatchFEs($MISSING_LABEL,\%goldFeFeats,$gfeat,$sent) if $VERBOSE;
                    
                }
            }
        }

        # Loop though scoreFrFeats to report features
        # that were not matches
	if ($DEBUG || $VERBOSE) {
            foreach $feature (sort keys %scoreFrFeats) {
		if (!defined($goldFrFeats{$feature})) {
		    my $sfr;
                    foreach $sfr (keys %{$scoreFrFeats{$feature}}) {
                        my $sscore = $scoreFrFeats{$feature}->{$sfr};
		    
                        print "extra frame: $feature $sfr ($sscore pts)\n" if $DEBUG;
                        printMatch("extra: $feature $sfr",0,$sscore,0) if $VERBOSE;

                        # NE/SUPPs don't have FEs
                        if ($feature =~ /=(NE|SU)$/) {
                            next;
                        }
                        my $sfeat = $feature .'='.$sfr;
                        &PrintNoMatchFEs($EXTRA_LABEL,\%scoreFeFeats,$sfeat,$sent) if $VERBOSE;
                    }
                }
            }
        }
        &PrintScoreTotal($matchSum,$scoreSum,$goldSum) if $VERBOSE;

        $totalMatchSum += $matchSum;
        $totalScoreSum += $scoreSum;
        $totalGoldSum += $goldSum;

        my $precision = 0;
	my $recall = $matchSum / $goldSum;
        my $fscore = 0;

        if ($scoreSum > 0) {
            $precision = $matchSum / $scoreSum;
        }
        if ($recall + $precision > 0) {
            $fscore = (2 * $recall * $precision) / ($recall + $precision);
        }
	if ($OUTPUT_PER_SENTENCE || $VERBOSE) {
            printf("Sentence ID=%d: Recall=%.${PREC}f (%.1f/%.1f) Precision=%.${PREC}f (%.1f/%.1f) Fscore=%.${PREC}f\n",
                   $sent, $recall, $matchSum, $goldSum, $precision, $matchSum, $scoreSum, $fscore);
        }
	$nSent++;
    }
    my $totalRecall = 0;
    my $totalPrecision = 0;
    my $fScore = 0;
    if ($totalGoldSum > 0) {
        $totalRecall = $totalMatchSum / $totalGoldSum;
    }
    if ($totalScoreSum > 0) {
        $totalPrecision = $totalMatchSum / $totalScoreSum;
    }
    if ($totalPrecision + $totalRecall > 0) {
        $fScore = (2 * $totalPrecision * $totalRecall ) / ($totalPrecision + $totalRecall);
    }

    print("Command: $PROG ".$CMDOPTIONS."\n");
    print("Input file: $f2\n");
    printf("%d Sentences Scored: Recall=%.${PREC}f (%.1f/%.1f)  Precision=%.${PREC}f (%.1f/%.1f)  Fscore=%.${PREC}f\n",
           $nSent,$totalRecall, $totalMatchSum, $totalGoldSum,
           $totalPrecision, $totalMatchSum, $totalScoreSum, $fScore);

    exit;
}
# end MAIN

# ################################################################
# ################################################################
# ################################################################
# ################################################################

#
# Given two associate array references: $gfH and $sfH, it attempts to
# find proper match-ups.
#
sub FindPairs {

    my ($type, $gfH, $sfH, $lookup) = @_;

    my @pairs;
    my @extraG;

    my %sFs = %{$sfH};

    my $nonexact = 0;

    # only FR and FEs can have non-exact matches
    if ($type eq "FE" || $type eq "FR") {
	$nonexact = 1;
    }
    
    my $gf;
    foreach $gf (keys %{$gfH}) {
        # exact match
        if (defined $sFs{$gf}) {
            my @pair = ($gf, $gf);
            push(@pairs, \@pair);
            delete $sFs{$gf};
            next;
        }
        
	if ($nonexact) {
	    # find closest
	    my $sf;
	    my $closestF;
	    my $closestFDist;
	    foreach $sf (keys %sFs) {
		if (defined $lookup->{$gf}->{$sf}) {
		    if ($closestF) {
			if ($lookup->{$gf}->{$sf} < $closestFDist) {
			    $closestF = $sf;
			    $closestFDist = $lookup->{$gf}->{$sf};
                    }
		    } else {
			$closestF = $sf;
			$closestFDist = $lookup->{$gf}->{$sf};
		    }
		}
	    }
	    if ($closestF) {
		my @pair = ($gf, $closestF);
		push(@pairs, \@pair);
		delete $sFs{$closestF};
		next;
	    }
	}

        push(@extraG,$gf);
    }

    my @extraS = keys %sFs;
    
    return (\@pairs, \@extraG, \@extraS);
}

###########################################################################
#
# FUNCTIONS THAT HELP DISPLAY COMPARISON RESULTS
#
###########################################################################

sub PrintNoMatchFEs {
    
    my ($type, $FEs, $feFeat, $sent) = @_;

    if (defined $FEs->{$feFeat}) {
        my $feSpan;
        foreach $feSpan (sort keys %{$FEs->{$feFeat}}) {
	    my $fe;
            foreach $fe (sort keys %{$FEs->{$feFeat}->{$feSpan}}) {
		my $score = $FEs->{$feFeat}->{$feSpan}->{$fe};
		if ($type == $EXTRA_LABEL) {
		    printMatch("  extra: $feFeat $feSpan $fe",0,$score,0);
		} else {
		    printMatch("  missing: $feFeat $feSpan $fe",0,0,$score);
		}
	    }
        }
    }
}

#
# Returns FrFeature total Score
#
sub sumFrFeatValues {

    my $hashRef = shift @_;
    
    my $sum = 0;
    my $num;
    my $key1;
    my $key2;

    foreach $key1 (keys %{$hashRef}) {
	foreach $key2 (keys %{$hashRef->{$key1}}) {
	    $sum += $hashRef->{$key1}->{$key2};
	}
    }
    return $sum;
}

#
# Returns FeFeature total Score
#
sub sumFeFeatValues {

    my $hashRef = shift @_;
    
    my $sum = 0;
    my $num;
    my $key1;
    my $key2;
    my $key3;

    foreach $key1 (keys %{$hashRef}) {
	foreach $key2 (keys %{$hashRef->{$key1}}) {
	    foreach $key3 (keys %{$hashRef->{$key1}->{$key2}}) {
		$sum += $hashRef->{$key1}->{$key2}->{$key3};
	    }
	}
    }
    return $sum;
}

#
# prints a sentence's words given a sentence hash (indexed by node id)
#
sub printSentence {

    my ($sent, $text, $words) = @_;

    if (defined $words->{$sent}) { # then print corresponding nodes
        
        my $node;
        my $wStr;
        my $nStr;
        foreach $node (sort keys %{$words->{$sent}}) {
            my $word = $words->{$sent}->{$node};
            my $wLen = length($word);
            my $nLen = length($node);
            if ($wLen >= $nLen) {
                $wStr .= $word . " ";
                $nStr .= $node . (" "x($wLen - $nLen)) . " ";
            } else {
                $wStr .= $word . (" "x($nLen - $wLen)) . " ";
                $nStr .= $node . " ";
            }
        }
        print $wStr . "\n";
        print $nStr . "\n";

    } else { # print span numbers

        my $len = length($text->{$sent});

        print $text->{$sent} . "\n";
        
        my $str = "0";
        my $slen = 1;
        my $index;
        while ($slen < $len) {
            if ($slen % 5 == 0) {
                $index = "$slen";
                $str .= $slen;
                $slen = length($str);
            } else {
                $str .= " ";
                $slen++;
            }
        }
        print $str."\n";
    }
}


# OBSOLETE:
#
# Replace node labels with their corresponding words to make a 
# pretty feature string for printing.

sub prettyFeature {

    my ($fStr,$text) = @_;
    my $start;
    my $end;
    my $pStr;
    
    if ($fStr =~ /.*\[(\d+.*\d+)\](;FRAME|)$/) {
        my @bounds = split(",",$1);
        my $bStr;
        for (my $i=0; $i <= $#bounds; $i++) {
            my ($start, $end) = split("-",$bounds[$i]);
            if ($bStr) {
                $bStr .= " ";
            }
            $bStr .= substr($text,$start,($end-$start)+1);
        }
        $pStr = $fStr . "(" . $bStr;
        
        if ($FEATURE_WIDTH - length($pStr) > 10) {
            $pStr .= ")";
        } else {
            $pStr = substr($pStr,0,$FEATURE_WIDTH-14);
            $pStr .= "...)";
        }
    } else {
        $pStr = $fStr . "()";
    }

    return($pStr);
}

#
# Prints a feature match result
#
sub printMatch {

    my ($feature, $score, $sscore, $gscore) = @_;

    my $pStr;
    my $buf;
    my $fLen = length($feature);

    if ($fLen > $FEATURE_WIDTH) {
	$pStr = substr($feature,0,$FEATURE_WIDTH - 3).'...';
    } else {
        $pStr = $feature . (" " x ($FEATURE_WIDTH - $fLen));
    }
    
    my $scoreStr = "";
    
    unless ($score==0 && $sscore==0 && $gscore==0) {
	$scoreStr = sprintf("%.${PREC}f / %.${PREC}f / %.${PREC}f",$score,$sscore,$gscore);
    }

    if ($feature eq "ColumnLabels") {
        $pStr = "MatchType: Feature";
        $buf = " " x ($FEATURE_WIDTH - length($pStr));
        $pStr .= $buf."  ";
        $buf = "    M    /    S    /    G    ";
        $pStr .= $buf;
    } else {
	$pStr .= "  ".$scoreStr;
    }
    
    print $pStr."\n";
}

# 
# auxiliary to printFeature: prints sums of each of the score columns;
#
sub PrintScoreTotal {

    my ($m, $s, $g) = @_;
    my $pstr = " " x $FEATURE_WIDTH;
    $pstr .= "  ";
    $pstr .= ("-" x $SCORE_WIDTH)."\n";
    $pstr .= (" " x ($FEATURE_WIDTH-6))."Total:  ";
    $pstr .= sprintf("%.${PREC}f / %.${PREC}f / %.${PREC}f", $m, $s, $g);

    print $pstr."\n";
}

##############################################################################
#
# FUNCTIONS FOR DETERMINING CORE/NON-CORE FE
#
##############################################################################

#
#  CreateFELookup( $fileName ) - reads frames.xml (as arg) and
#     returns a hash with core-FEs defined as keys
#
sub CreateFELookup {

    my $fileName = shift(@_);
    my %coreLookup;

    # initialize parser and read the file
    my $parser = new XML::Parser( Style => 'Tree' );
    my $tree = $parser->parsefile( $fileName );

    my @frames = @{$tree->[1]};
    
    # read content of <frames>
    for (my $f=0; $f<=$#frames; $f++) {

	if ($frames[$f] eq 'frame') {
	    $f++;
	    my @frame = @{$frames[$f]};
	    my %fatts = %{$frame[0]};
	    my $frName = $fatts{name};

	    print "parsing frame $frName ...\n" if ($DEBUG > 1);

	    # read content of <frame>
	    for (my $e=0; $e <= $#frame; $e++) {
		if ($frame[$e] eq 'fes') {
		    $e++;
		    my @fes = @{$frame[$e]};

		    # read content of <fes>
		    for (my $s=0; $s<=$#fes; $s++) {
			
			# each fe
			if ($fes[$s] eq 'fe') {
			    $s++;
			    my %featts = %{$fes[$s]->[0]};
			    my $feName=$featts{name};
			    my $feType=$featts{coreType};
			    print "FE $feName is of type $feType\n" if ($DEBUG > 1);
			    if ($feType eq 'Core') {
				# add lookup for Core FEs
				$coreLookup{$frName.'='.$feName} = 1;
			    }
			} # each fe
		    } # <fes>
		}
	    } # <frame>
	}
    } # <frames>

    return (%coreLookup);
}

#
# Return points for an FE
#
sub FePoints {
    my ($frame, $fe) = @_;

    if (defined $CORELOOKUP{$frame.'='.$fe}) {
        return $COREFE_POINTS;
    } else {
        return $NONCOREFE_POINTS;
    }
}

###########################################################################
#
#    FUNCTIONS FOR PARSING FULL-TEXT XML FOR SCORING
#
###########################################################################

# FRAMENET FULL-TEXT XML
#
# ParseFNXMLFile ( filename ) - parses xml file and extracts information
#    relevant for scoring in a format conducive to comparison
#  
#   returns 3 references to hashes:
#      sentFeatures - indexed by sentence ID, each value is also a hash.
#                     each of those hashes is indexed by a feature composed of
#                     of LayerType, Frame, LabelName, Range,
#                     with the values being the score that each feature is worth.
#      sentTextLookup - indexed by sentence ID, each value is a string with
#                       that sentence's text
#
sub ParseFNXMLFile {

    my $fileName = shift(@_);

    # initialize parser and read the file
    my $parser = new XML::Parser( Style => 'Tree' );
    my $tree = $parser->parsefile( $fileName );

    # list of sentences
    my $corpus = $tree->[1];

    # sent ID are assigned sequentially (ignore any existing ID)
    my $sentID = -1;
    my %sentFrFeatures;
    my %sentFeFeatures;
    my %sentTextLookup;

    # sanity check
    if ($corpus->[3] ne 'documents') {
        die "ERROR: FrameNet FT XML Parser did not find a <documents> element in $fileName.\n";
    }

    my @documents = @{$corpus->[4]};
    for ( my $i=0; $i <= $#documents; $i++ ) {
        if ($documents[$i] eq 'document') {
            $i++;
            my $document = $documents[$i];
            my @paragraphs = @{$document->[4]};
            for (my $j=0; $j <= $#paragraphs; $j++) {
                if ($paragraphs[$j] eq 'paragraph') {
                    $j++;
                    my $paragraph = $paragraphs[$j];
                    my @sentences = @{$paragraph->[4]};
                  SLOOP:for (my $k=0; $k <= $#sentences; $k++) {
                      if ($sentences[$k] eq 'sentence') {
                          $k++;
                          $sentID++;

                          my %frFeatures;
                          
                          my @sentence = @{$sentences[$k]};
                          my $sentText = $sentence[4]->[2];

                          print "Found sentence: $sentText\n" if ($DEBUG > 1);

                          # this for look is needed even though there is only one
                          # <annotationSets> in order to skip optional elements (i.e. pos)
                          for (my $q=0; $q <= $#sentence; $q++) {
                              if ($sentence[$q] eq 'annotationSets') {
                                  $q++;
                                  
                                  my @annotationSets = @{$sentence[$q]};
                                  
                                  # AnnotationSet IDs are also assigned in sequence (ignore existing IDs)
                                  my $annoID = -1;
                                  
                                ASLOOP: for (my $l=0; $l <= $#annotationSets; $l++) {
                                    if ($annotationSets[$l] eq 'annotationSet') {
                                        $l++;
                                        $annoID++;
                                        
                                        my %feFeatures;
                                        my $frameTargetFeature;
                                        my $annotationSet = $annotationSets[$l];
                                        my $frameName;
                                        if (defined $annotationSet->[0]->{frameName}) {
                                            $frameName = $annotationSet->[0]->{frameName};
                                        } else {
                                            # if no frameName is given, it may contain NER labels
                                            $frameName = "NE";
                                        }
                                        my @layers = @{$annotationSet->[4]};
                                        for (my $m = 0; $m <= $#layers; $m++) {
                                            if ($layers[$m] eq 'layer') {
                                                $m++;
                                                my $layer = $layers[$m];
                                                my $layerName = $layer->[0]->{name};
                                                unless ($layerName eq 'Target' ||
                                                        $layerName eq 'FE' ||
                                                        $layerName eq 'NER') {
                                                    next;
                                                }
                                                # skip non-primary layers
                                                if (defined $layer->[0]->{rank}) {
                                                    if ($layer->[0]->{rank} ne "1") {
                                                        next;
                                                    }
                                                }
                                                # targets can be multiple pieces
                                                my @targetBounds;
                                                
                                                # each FE can have multiple pieces
                                                my %feBounds;
                                                
                                                if (!defined $layer->[4]) {
                                                    print "Frame $frameName AnnoSet $annoID Layer $layerName is empty.\n" if $DEBUG;
                                                    next;
                                                }
                                                my @labels = @{$layer->[4]};
                                                for (my $n = 0; $n <= $#labels; $n++) {
                                                    if ($labels[$n] eq 'label') {
                                                        $n++;
                                                        my $label = $labels[$n];
                                                        if (defined $label->[0]->{itype}) {
                                                            # skip INI, DNI, INC labels
                                                            next;
                                                        }
                                                        my $labelName = $label->[0]->{name};
                                                        my $labelBound = $label->[0]->{start}."-".
                                                            $label->[0]->{end};
                                                        
                                                        print "found $frameName $layerName $labelName $labelBound\n" if ($DEBUG);
                                                        
                                                        if ($layerName eq "Target") {
                                                            if ($frameTargetFeature) {
                                                                print STDERR "ERROR: More than one Target in AnnotationSet $annoID in Sentence $sentID\n";
                                                                print STDERR "ERROR: Skipping AnnotationSet $annoID.\n";
                                                                next ASLOOP;
                                                            }
                                                            if ($labelName eq "Target") {
                                                                push(@targetBounds, $labelBound);
                                                            }
                                                        } elsif ($layerName eq "FE") {
                                                            # each piece of a Path FE is treated separately
                                                            if ($labelName eq "Path") {
                                                                $feFeatures{$labelBound.'=FE'} = 
                                                                { $frameName.$SEPARATOR.$labelName => &FePoints($frameName,$labelName) };
                                                            } else {
                                                                # otherwise, need to collate the pieces
                                                                if (defined $feBounds{$labelName}) {
                                                                    $feBounds{$labelName} .= ','.$labelBound;
                                                                } else {
                                                                    $feBounds{$labelName} = $labelBound;
                                                                }
                                                            }
                                                        } elsif ($layerName eq "NER") {
                                                            $frFeatures{$labelBound.'=NE'} = { $labelName => $NE_POINTS };
                                                        }
                                                    }
                                                }
                                                
                                                # process list of targets
                                                if ($layerName eq "Target") {
						    my $feature = join(',',sort @targetBounds).'=FR';
                                                    $frameTargetFeature = $feature.'='.$frameName;
						    $frFeatures{$feature} = { $frameName => $FRAME_POINTS };
                                                }
                                                # process lists of FEs (except Path FEs, see above)
                                                if ($layerName eq "FE") {
                                                    my $fe;
                                                    foreach $fe (keys %feBounds) { # feBounds maps each FE name to a string representing its span(s)
                                                        my $bounds = join(',',sort(split(',',$feBounds{$fe})));

							# nschneid: allow for multiple frame elements to have the same predicted span ($feBounds value)
							my $feBounds = $bounds.'=FE';							
							if (!(defined $feFeatures{$feBounds})) {
							    $feFeatures{$feBounds} = {};
							}
							$feFeatures{$feBounds}->{$frameName.$SEPARATOR.$fe} = &FePoints($frameName,$fe);
                                                    }
                                                }
                                            }
                                            # end - each layer
                                        }
                                        
					if (defined($sentFeFeatures{$sentID})) {
					    $sentFeFeatures{$sentID}->{$frameTargetFeature} = \%feFeatures;
					} else {
					    $sentFeFeatures{$sentID} = { $frameTargetFeature => \%feFeatures };
					}
					
                                    }
                                    # end - each annotation set
                                }
                              }
                              # end - each annotationSets
                          }
                          #store in Sentence hash
                          $sentFrFeatures{$sentID} = \%frFeatures;
                          $sentTextLookup{$sentID} = $sentText;
                          
                      }
                      # end - each sentence
                  }
                }
            }
        }
    }
    
    return (\%sentFrFeatures, \%sentFeFeatures, \%sentTextLookup, {});
}


# SEM XML - FTTOSEM
# ParseSemXMLFile ( filename ) - parses xml file and extracts information
#    relevant for scoring in a format conducive to comparison
#    XML file must be the output of fttosem
#  
#   returns 3 references to hashes, all indexed by sentID:
#      sentFrFeatures - Frame/NE/Supp features
#      sentFeFeatures - FE features
#      sentWordContent - sentence text
#
sub ParseSemXMLFile {

    my $fileName = shift(@_);

    # initialize parser and read the file
    my $parser = new XML::Parser( Style => 'Tree' );
    my $tree = $parser->parsefile( $fileName );

    # list of sentences
    my @corpus = @{$tree->[1]};

    my %sentWordContent;
    my %sentWordLookup;
    my %sentFrFeatures;
    my %sentFeFeatures;

    my $sentID = -1;
    my $err;

    for ( my $i=0; $i <= $#corpus; $i++ ) {
        if ($corpus[$i] eq 'sentence') {
            $i++;
            $sentID++;

            my @sentence = @{$corpus[$i]};
            my %headWordLookup;
            my %wordLookup;
            my %frFeatures;
            my %feFeatures;
            my %suppFeatures;

            print "found sentence ID#".$sentID."\n" if $DEBUG;
            if ($DEBUG && defined($sentence[0]->{ID})) {
                print "Sentence $sentID has ID=".$sentence[0]->{ID}."\n";
            }

            for (my $j=1; $j <= $#sentence; $j++) {
                
                # Parse words--create wordLookup hash
		# words of compared sentences should be identical
		# create a string to compare
                if ($sentence[$j] eq 'words') {
                    $j++;
		    my $sentStr;
                    my @words = @{$sentence[$j]};
                    for (my $w=0; $w<=$#words; $w++) {
			# each word
                        if ($words[$w] eq "W") {
                            $w++;
                            my %watts= %{$words[$w]->[0]};
                            $wordLookup{$watts{ID}} = $watts{form};
                            $sentStr .= 'W'.$watts{ID}.$watts{start}
				.$watts{end}.$watts{form};
			    print "found word ".$watts{ID}."\n" if $DEBUG;
                            print "WORD $watts{ID} $wordLookup{$watts{ID}} \n" if $DEBUG;
                        }
                    }
		    $sentWordContent{$sentID} = $sentStr;
                }
                
                # Parse non-terminals
                if ($sentence[$j] eq 'non-terminals') {
                    $j++;
                    my @nts = @{$sentence[$j]};
                    for (my $n=0; $n <= $#nts; $n++) {

			# Each <P> Node
                        if ($nts[$n] eq 'P') {
                            $n++;
                            my @pnode = @{$nts[$n]};
                            my %patts = %{$pnode[0]};
                            my $frTarget;

                            print "found P node ".$patts{ID}."\n" if $DEBUG;
                            
			    # If <P> Node is a Frame:
			    #    create Frame Feature: Fname;Target worth FRAME_POINTS
			    #    create a headWordLookup entry for this node with Head=Target
			    #    if frame has a denoted FE, also create an FE Feature
                            if (defined $patts{Frame}) {
				#reformat Target: space separated
                                $frTarget = join(",",sort(split(/, /,$patts{Target})));

                                # Target of a Frame is always a HEAD (also for NEs?)
                                $headWordLookup{$patts{ID}} = $frTarget;

				# Named Entities are counted differently from Frames
				if ($patts{Frame} =~ /^NE:/) {
                                    if($err = &AddFrFeature(\%frFeatures, "NE", $patts{Frame}, $frTarget, $NE_POINTS)) {
                                        print STDERR "ERROR: sent $sentID in $fileName: $err\n";
                                    }
				    print "NE $patts{Frame} $patts{Target}\n" if $DEBUG;
				} else {
                                    if ($err = &AddFrFeature(\%frFeatures, "FR", $patts{Frame}, $frTarget, $FRAME_POINTS)) {
                                        print STDERR "ERROR: sent $sentID in $fileName: $err\n";
                                    }
				    print "Frame $patts{Frame} $patts{Target}\n" if $DEBUG;

				    # Denoted FEs (there can be multiple--comma separated)
				    if (defined $patts{Denoted}) {                                        
					my @denotedFEs = split(/, /,$patts{Denoted});

                                        # take only the first item if there are multiple
					my $dfe = $denotedFEs[0];;
                                        $feFeatures{$frTarget.$SEPARATOR.$patts{Frame}.$SEPARATOR.$dfe} = $frTarget;
				    }
				}
                            }
                            
                            # parse E Tags
                            my $headFound = 0;
                            my $semHeadFound = 0;
                            my $eRefs;
                            for (my $p=0; $p<$#pnode; $p++) {

                                # EACH E NODE:
                                if ($pnode[$p] eq 'E') {
                                    $p++;
                                    my @enode=@{$pnode[$p]};
                                    my %eatts = %{$enode[0]};

                                    # record list of all Refs to use as headlist in cases where no head is marked
                                    if (defined $eatts{Ref}) {
                                        if ($eRefs) {
                                            $eRefs .= ",".$eatts{Ref};
                                        } else {
                                            $eRefs = $eatts{Ref};
                                        }
                                    }
                                    
                                    # if frame element, then record in feFeatures
                                    if (defined $eatts{FE}) {
                                        # If multiple instances of the same FE label: they are multipart
                                        if (defined $feFeatures{$frTarget.$SEPARATOR.$patts{Frame}.$SEPARATOR.$eatts{FE}}) {
                                            $feFeatures{$frTarget.$SEPARATOR.$patts{Frame}.$SEPARATOR.$eatts{FE}} .= ",".$eatts{Ref};
                                        } else {
                                            $feFeatures{$frTarget.$SEPARATOR.$patts{Frame}.$SEPARATOR.$eatts{FE}} = $eatts{Ref};
                                        }
                                        print "  FE $eatts{FE} $eatts{Ref}\n" if $DEBUG;
                                    }
                                    
                                    # Record in headWordLookup for the <P> containing this element
                                    if ($eatts{Head} eq "True" || $eatts{SemHead} eq "True") {

                                        if ($eatts{Head} eq "True") {
                                            $headFound = 1;
                                        } else {
                                            $semHeadFound = 1;
                                        }
                                        if (defined $headWordLookup{$patts{ID}}) {
                                            $headWordLookup{$patts{ID}} .= ",".$eatts{Ref};
                                        } else {
                                            $headWordLookup{$patts{ID}} = $eatts{Ref};
                                        }
                                    }

				    # If we are looking at an <E> inside a Supp <P>
				    # Record Supp features, all such <E>'s should be SemHeads

				    if (defined $patts{Supp}) {
					print STDERR "ERROR: sent $sentID in $fileName: non-SemHead <E> inside a Supp <P>\n" unless $eatts{SemHead} eq "True";
					if (defined $suppFeatures{"Supp".'='.$patts{Supp}}) {
					    $suppFeatures{"Supp".$SEPARATOR.$patts{Supp}} .= ",".$eatts{Ref};
					} else {
					    $suppFeatures{"Supp".$SEPARATOR.$patts{Supp}} = $eatts{Ref};
					}
				    }

                                } # end of each E node
                            }
                            # validity check for each P node
                            if (defined($patts{Frame}) && ($headFound + $semHeadFound) > 0) {
                                print STDERR "ERROR: sent $sentID in $fileName: (sem)headwords found for non-transparent frame P ID=".$patts{ID}."\n"
				    unless ($patts{Transparent} eq "True");
                            }
                            if (defined($patts{Frame}) && $patts{Transparent} eq "True" and $semHeadFound==0) {
                                print STDERR "ERROR: sent $sentID in $fileName: no semheadwords found for transparent frame P ID=".$patts{ID}."\n";
                            }
                            if (!defined($patts{Frame}) && ($headFound + $semHeadFound)==0) {
                                print "No headwords are marked: using all edges as heads, sent $sentID in $fileName\n" if $DEBUG;
                                $headWordLookup{$patts{ID}} = $eRefs;
                            }
                            
                            print "HeadWords for ".$patts{ID}.": ".$headWordLookup{$patts{ID}}."\n\n" if $DEBUG;

                        } # end of each P node
                    }
                    last; # bc. there's only 1 <non-terminals>
                }
            }

            # generate feature set

	    # for fe features, must resolve headwords using the lookups
            my $feFeat;
            my %procFeFeatures;

            foreach $feFeat (keys %feFeatures) {
                my $frTarget;
                my $feHWs;
                my $frame;
                my $fe;
		($frTarget,$frame,$fe) = split(/$SEPARATOR/,$feFeat);
                $feHWs = $feFeatures{$feFeat};
                $feHWs = join(",",&resolveHeadWords($feHWs,\%headWordLookup, \%wordLookup));
                if ($err = &AddFeFeature(\%procFeFeatures,$frame,$frTarget,$fe,$feHWs)) {
                    print STDERR "ERROR: sent $sentID in $fileName: $err\n";
                }
            }
        
	    # for supp features
            my $supp;
	    foreach $supp (keys %suppFeatures) {
		my $suppHeads = join(",",&resolveHeadWords($suppFeatures{$supp},\%headWordLookup, \%wordLookup));
                if ($err = &AddFrFeature(\%frFeatures, "SU", $supp, $suppHeads, $SUPP_POINTS)) {
                    print STDERR "ERROR: sent $sentID in $fileName:  $err\n";
                }
	    }

            #store in Sentence hash
            $sentFrFeatures{$sentID} = \%frFeatures;
            $sentFeFeatures{$sentID} = \%procFeFeatures;
            $sentWordLookup{$sentID} = \%wordLookup;

        } # end of each sentence
        
    }
    if ($sentID == -1) {
        die "ERROR: SemXML Parser found no sentences found in $fileName.\n";
    }
    return (\%sentFrFeatures, \%sentFeFeatures, \%sentWordContent, \%sentWordLookup);
}

#
# $featHash must be a reference to an associative array
#
sub AddFrFeature {

    my ($featHash, $type, $frame, $target, $points) = @_;

    if ($type eq "NE") {
        $frame =~ s/^NE://;
    }
    my $feat = $target.'='.$type;
    if (defined $featHash->{$feat}) {
        if (defined $featHash->{$feat}->{$frame} && $type ne "SU") {
            return "multiple instances of $frame on target $target";
        }
        $featHash->{$feat}->{$frame} = $points;
    } else {
        $featHash->{$feat} = { $frame => $points };
    }
    
    return "";
}

#
# $featHash must be a reference to an associative array
#
sub AddFeFeature {

    my ($featHash, $frame, $frTarget, $fe, $feSpan) = @_;

    my $frFeat = $frTarget.'=FR='.$frame;
    my $feFeat = $feSpan.'=FE';

    if (defined $featHash->{$frFeat}) {
        if (defined $featHash->{$frFeat}->{$feFeat}) {
	    if (defined $featHash->{$frFeat}->{$feFeat}->{$frame.$SEPARATOR.$fe}) {
		return "multiple instances of FE ($fe) on $feSpan at $frFeat";
	    }
            $featHash->{$frFeat}->{$feFeat}->{$frame.$SEPARATOR.$fe} = &FePoints($frame,$fe);
        } else {
            $featHash->{$frFeat}->{$feFeat} = { $frame.$SEPARATOR.$fe => &FePoints($frame,$fe) };
        }
    } else {
        $featHash->{$frFeat} = { $feFeat => { $frame.$SEPARATOR.$fe => &FePoints($frame,$fe) } };
    }

    return "";
}

#
# Recursive procedure for resolving head words
#
sub resolveHeadWords  {

    my ($str, $HwHash, $WHash) = @_;

    my @refs = split(/[, ]/,$str);
    my @retList;
    my $ref;

    print "resolveHeadsCalled on $str\n" if $DEBUG;
    
    foreach $ref (@refs) {
        if ($WHash->{$ref}) {
            push(@retList, $ref);
        } elsif ($HwHash->{$ref}) {
            if ($HwHash->{$ref} eq $ref) {
                # self-referential non-terminal node
                print STDERR "ERROR: Circular reference in non-terminal node $ref.\n";
            } else {
                push(@retList, &resolveHeadWords($HwHash->{$ref}, $HwHash, $WHash));
            }
        } else {
            print STDERR "ERROR: Cannot resolve HW for $ref--neither a P with a HW nor a W\n";
        }
    }
    
    my %uniq;
    foreach $ref (@retList) {
        $uniq{$ref} = 1;
    }

    return (sort keys %uniq);

}


##################################################################################
# 
# FUNCTIONS FOR READING IN / COMPUTING FRAME AND FE RELATIONS
# 
##################################################################################

sub ParseProcessFrRelXML {
    
    my $frRelFile = shift @_;

    my %frSPLength;
    my %frSPCount;
    my %feSPLength;
    my %feSPCount;

    my $cacheFile = $CACHEDIR."/$PROG.cache";

    if ($NOPARTIALCREDIT) {
        # return empty hashes
        return(\%frSPLength,\%frSPCount,\%feSPLength,\%feSPCount);
    }

    if (-e $cacheFile) {
        print "Retrieving SP information from cache.\n" if $DEBUG;

        my ($frSPL, $frSPC, $feSPL, $feSPC) = @{retrieve($cacheFile)};
        return($frSPL, $frSPC, $feSPL, $feSPC);
    }

    my ($frConnL, $feConnL) = &ParseFrRelationsXML($frRelFile);

    my $depth = 0;
    my $frame;

    foreach $frame (keys %{$frConnL}) {
        my %distLookup;
	my %nPathLookup;
        my @startNodes = ($frame);
        
	print "Frame $frame: \n" if $DEBUG;
        &ComputeSP(\%distLookup,\%nPathLookup,\@startNodes,$frConnL,0);
        $frSPLength{$frame} = \%distLookup;
	$frSPCount{$frame} = \%nPathLookup;
    }
    
    $depth = 0;
    my $fe;
    foreach $fe (keys %{$feConnL}) {
        my %distLookup;
	my %nPathLookup;
        my @startNodes = ($fe);
        
	print "FE $fe: \n" if $DEBUG;
        &ComputeSP(\%distLookup,\%nPathLookup,\@startNodes,$feConnL,0);
        $feSPLength{$fe} = \%distLookup;
	$feSPCount{$fe} = \%nPathLookup;
	
    }

    if (!-e $cacheFile) {
	if (-e $CACHEDIR) {
	    my @cache = (\%frSPLength,\%frSPCount,\%feSPLength,\%feSPCount);
	    store(\@cache, $cacheFile);
	    print "Created cache file $cacheFile\n";
	} else {
	    print STDERR "WARNING: directory $CACHEDIR doesn't exist.  No FrRelation cache saved.\n";
	}
    }
    
    return(\%frSPLength,\%frSPCount,\%feSPLength,\%feSPCount);
}

#
# ComputeSP
#
# computes distances from an Origin to successively farther connected nodes,
# saving only the shortest distance.  Finds shortest paths and the number of such
# paths.
#
# $processed must be a pointer to an associative array
# $numPaths must be a pointer to an associative array
# $current must be a pointer to a list of all nodes at the present level
#
sub ComputeSP {
    my ($processed, $numPaths, $current, $lookup, $depth) = @_;

    my @recurselist;

    my $cnode;
    foreach $cnode (@{$current}) {
        if (defined $processed->{$cnode}) {
	    # if found that a path of the same depth already exists
	    # increment the number of shortest paths count
            if ($processed->{$cnode} == $depth) {
		$numPaths->{$cnode}++;
	    }
	    next;
        } else {
	    $processed->{$cnode} = $depth;
	    $numPaths->{$cnode} = 1;

	    print "  --> $cnode by $depth\n" if $DEBUG;
            push (@recurselist, @{$lookup->{$cnode}});
        }
    }

    if ($depth < $MAX_DISTANCE) {
      &ComputeSP($processed, $numPaths, \@recurselist, $lookup, $depth + 1);        
    }

}

sub GetSP {

    my ($n1, $n2, $lookup) = @_;
    my $node = $n1;
    my $dist = $lookup->{$n1}->{$n2};
    my @path = ($n1);
    my $nextNode;

 STEP:while ($dist > 0) {
	if ($node eq $n2) {
	    $dist = 0;
	    next STEP;
	}
	# find all the nodes that are 1 away from $node
	foreach $nextNode (keys %{$lookup->{$node}}) {
	    if ($lookup->{$node}->{$nextNode} == 1) {
		if ($lookup->{$nextNode}->{$n2} == ($dist-1)) {
		    $node = $nextNode;
		    $dist--;
		    push (@path, $node);
		    next STEP;
		}
	    }
	}
	# should never get here
	print STDERR "ERROR: no path from $n1 -> $n2: only ".join("->",@path)."\n";
        return (@path);
    }
    
    return (@path);
}

#
#  CreateFELookup( $fileName ) - reads frames.xml (as arg) and
#     returns a hash with core-FEs defined as keys
#
sub ParseFrRelationsXML {

    my $fileName = shift(@_);

    # initialize parser and read the file
    my $parser = new XML::Parser( Style => 'Tree' );
    my $tree = $parser->parsefile( $fileName );

    my @frRelations = @{$tree->[1]};

    my %frConnLookup;
    my %feConnLookup;

    # read content of <frames>
    for (my $i=0; $i<=$#frRelations; $i++) {
        if ($frRelations[$i] eq 'frame-relation-type') {
            $i++;
            
            my $frameRelationType = $frRelations[$i];
            my $frRelType = $frameRelationType->[0]->{name};
            print "Found frame-relaton-type: ".$frRelType."\n" if $DEBUG;

            # pretend that 'See_also' doesn't exist
	    # Also need to ignore Intra-Frame FE Relations
            if ($frRelType eq 'See_also' ||
		$frRelType eq 'CoreSet' ||
		$frRelType eq 'Excludes' ||
		$frRelType eq 'Requires') {
                next;
            }
            
            my @frameRelations = @{$frameRelationType->[4]};
            for (my $j=0; $j<=$#frameRelations; $j++) {
                if ($frameRelations[$j] eq 'frame-relation') {
                    $j++;

                    my @frameRelation = @{$frameRelations[$j]};
                    my $superF = $frameRelation[0]->{superFrameName};
                    my $subF = $frameRelation[0]->{subFrameName};
                    
                    if (!defined($frConnLookup{$superF})) {
                        $frConnLookup{$superF} = [$subF];
                    } else {
                        push(@{$frConnLookup{$superF}}, $subF);
                    }
                    if (!defined($frConnLookup{$subF})) {
                        $frConnLookup{$subF} = [$superF];
                    } else {
                        push(@{$frConnLookup{$subF}}, $superF);
                    }
                    
                    print "  Connected $subF and $superF\n" if $DEBUG;
                    
                    for (my $k=0; $k <= $#frameRelation; $k++) {
                        if ($frameRelation[$k] eq 'fe-relation') {
                            $k++;
                            my $feRelation = $frameRelation[$k]->[0];
                            my $superFE = $superF.';'.$feRelation->{superFEName};
                            my $subFE = $subF.';'.$feRelation->{subFEName};

                            if (!defined($feConnLookup{$superFE})) {
                                $feConnLookup{$superFE} = [$subFE];
                            } else {
                                push(@{$feConnLookup{$superFE}}, $subFE);
                            }
                            if (!defined($feConnLookup{$subFE})) {
                                $feConnLookup{$subFE} = [$superFE];
                            } else {
                                push(@{$feConnLookup{$subFE}}, $superFE);
                            }
                            
                        }
                    }
                }
                
            }

        }
    }
    # end of parsing XML

    return (\%frConnLookup, \%feConnLookup);
}






sub usage {
    my $usage = <<USAGEEND;

usage: $PROG [options] <frames.xml> <frRelations.xml> <goldstandard.xml> <file-to-score.xml>
     
   Script for scoring semantic role labeling.
   Requires 4 arguments:

   <frames.xml>       : frXML/frames.xml file from FrameNet data.
                        This file can be downloaded from the
                        FrameNet public website at the following URL:
                        http://framenet.icsi.berkeley.edu/fnUsers/dataRequest

   <frRelations.xml>  : frXML/frRelations.xml file from FrameNet data.
                        See <frames.xml> on how to obtain.

   <goldstandard.xml> : semXML containing extracted structure from gold
                        standard FrameNet full-text output XML
                        
                        OR (if -f option is given)

                        FrameNet Full-Text Annotation XML containing gold
                        standard annotation..

   <file-to-score.xml>: file to score, semXML

                        OR (if -f option is given)

                        file to score, FrameNet Full-Text Annotatation XML

   The script calculates (aggregated) recall and precision, and an
   fscore value calculated over all sentences in the file to score.
   Note: by default the input format for gold standard annotation as
   well as the annotation being scored is the semXML format output by
   fttosem.

 Options:

   -c <dir> : specify directory where Frame Relation path information
              will be cached.  Presently: $CACHEDIR.

   -e : exact matches only; turn off partial credit scoring

   -h : displays this usage statement

   -l : labels only; score FrameNet Full-Text XML instead of fttosem's semXML

   -n : no scoring of Named Entities (the default is to include them in 
        the score)

   -s : print recall, precision, and fscore for each sentence 
        (by default, these are only printed for the file as a whole)

   -t : score only Frame Target matches (no scoring of FEs or NEs)

   -v : produce verbose output, e.g. sentence text and information about which
        features matched and which did not.  Scores for missed features
        appear under "G", for matching features under "M", and for extra
        features (in the score file but not in the gold standard) under "S".
        The "totals" calculated for "G" and "S" include the sum of the
        points for matches, i.e. "M".

USAGEEND
		
    die $usage;
}
