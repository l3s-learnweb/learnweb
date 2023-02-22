#!/bin/bash
set -e

# keep a sentinel file so we don't try to create the core a second time for example when we restart a container
sentinel=/var/solr/core_created
if [ -f $sentinel ]; then
    echo "skipping core creation"
else
    # Could set env-variables for solr-fg
    source run-initdb

    # get the core name.
    solr_host="localhost:${SOLR_PORT:-8983}"
    core_name=$(ls -AU /var/solr/data/ | head -1)
    if [[ -z $core_name ]]; then
        echo "could not determine core name"
        exit 1
    fi

    echo "core detected: $core_name"

    sed -i '/<!-- <lib dir="${solr.install.dir:..\/..\/..\/..}\/modules\/ltr\/lib" regex=".*\\.jar" \/> -->/a\
  <lib dir="${solr.install.dir:..\/..\/..}\/modules\/extraction\/lib" regex=".*\\.jar" \/>' /var/solr/data/$core_name/conf/solrconfig.xml

    # start a Solr so we can use the Schema API, but only on localhost
    echo "starting local solr"
    start-local-solr &
    echo "waiting 10 seconds for solr to start"
    sleep 10

    # Now configure with the Schema API

    echo "adding /update/extract request handler"
    curl -X POST -H 'Content-type:application/json' -d '{
      "add-requesthandler": {
        "name": "/update/extract",
        "startup": "lazy",
        "class": "solr.extraction.ExtractingRequestHandler",
        "defaults":{ "lowernames": "true", "fmap.content":"_text_"}
      }
    }' http://$solr_host/solr/$core_name/config

    echo "adding /LearnwebQuery request handler"
    curl -X POST -H 'Content-type:application/json' -d '{
      "add-requesthandler": {
        "name": "/LearnwebQuery",
        "class": "solr.SearchHandler",
        "defaults": {
          "echoParams": "explicit",
          "defType": "edismax",
          "qf": "title^4.0 tags^3.0 description^2.5 comments^2.0 machineDescription^1.0",
          "pf": "title tags description comments machineDescription",
          "tie": 0.1,
          "q.alt": "*:*",
          "fl": "*,score",
          "mm": "-20%"
        }
      }
    }' http://$solr_host/solr/$core_name/config

    echo "adding schema fields"
    curl -X POST -H 'Content-type:application/json' --data-binary '{
     "add-field":[
      {
      "name": "author",
      "type": "text_general",
      "multiValued": false
    },
    {
      "name": "comments",
      "type": "text_general"
    },
    {
      "name": "description",
      "type": "text_general",
      "multiValued": false
    },
    {
      "name": "format",
      "type": "string"
    },
    {
      "name": "groupId",
      "type": "pint"
    },
    {
      "name": "language",
      "type": "string"
    },
    {
      "name": "location",
      "type": "string"
    },
    {
      "name": "machineDescription",
      "type": "text_general",
      "multiValued": false
    },
    {
      "name": "ownerUserId",
      "type": "pint"
    },
    {
      "name": "path",
      "type": "string"
    },
    {
      "name": "source",
      "type": "string"
    },
    {
      "name": "tags",
      "type": "text_general"
    },
    {
      "name": "text",
      "type": "text_general",
      "multiValued": true,
      "indexed": true,
      "stored": false
    },
    {
      "name": "timestamp",
      "type": "pdate"
    },
    {
      "name": "title",
      "type": "text_general",
      "multiValued": false
    },
    {
      "name": "type",
      "type": "string"
    },
    {
      "name": "url",
      "type": "string"
    }
     ],
     "add-copy-field":[
      {
      "source": "author",
      "dest": "text"
    },
    {
      "source": "comments",
      "dest": "text"
    },
    {
      "source": "description",
      "dest": "text"
    },
    {
      "source": "machineDescription",
      "dest": "text"
    },
    {
      "source": "tags",
      "dest": "text"
    },
    {
      "source": "title",
      "dest": "text"
    },
    {
      "source": "author",
      "dest": "author_s"
    },
    {
      "source": "tags",
      "dest": "tags_ss"
    },
    {
      "source": "*",
      "dest": "_text_"
    }
     ]
   }' http://$solr_host/solr/$core_name/schema

    echo "finished configuring with the Schema API"

    stop-local-solr
    sleep 1

    touch $sentinel
fi
