# rhizome

This software does the pre-processing necessary to use the iris latent
topic feedback plugin for information retrieval.  At a high-level, the
intended workflow is:

1. User ingests corpus into Solr
2. Run rhizome against Solr to populate a MongoDB instance with LDA topics
3. Run iris as part of your Solr-based document search system

This system is based on the KDD paper:

Latent Topic Feedback for Information Retrieval
David Andrzejewski and David Buttler.
Proceedings of the 17th ACM SIGKDD Conference on Knowledge Discovery
and Data Mining (KDD 2011)

The iris code was developed by Kevin R. Lawrence, and the rhizome
pre-processing module was written by David Andrzejewski.

## Usage

This code can be called from the command-line, an example use case
is given in runme.sh

The following operations are used to populate a running MongoDB instance
with the information Iris will need to function:

- count: count token frequence to help determine rare words cutoff threshold
- stop: use rare words cutoff threshold to construct a stoplist
- lda: run LDA on the corpus
- turbo: identify significant n-grams for each topic
- related: use topic-topic covariance to identify related topics for each topic
- semco: calculate semantic coherence scores for each topic

The following command-line options (with defaults in parentheses)
allow the user to specify parameters of the MongoDB instance, the Solr
index, and the LDA topic model:

```
mongohost (localhost) = MongoDB host 
mongoport (27017) = MongoDB port 
mongoname (topics) = MongoDB database name 
solrhost (localhost) = Solr index address 
solrport (8983) = Solr index port 
solrfields (title,text) = Comma-separated list of Solr fields to model 
solrtitle (nil) = Solr field to use as document names
stoplow (0) = Low end of stoplist count thresholds to print out for 'count'
stophigh (100) = High end of stoplist thresholds to print out for 'count'
stopthresh (50) = Filter out rare words occurring < stopthresh times 
T (100) = Number of latent topics to use 
nsamp (1000) = Number of MCMC samples to take 
```

## License

This code is licensed under the terms of the GNU GPL license, the the [LICENSE.txt](/LICENSE.txt) file for full details.

Copyright (c) 2012, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory. Written by David Andrzejewski <david.andrzej@gmail.com>

LLNL-CODE-521811 All rights reserved. This file is part of IRIS.
