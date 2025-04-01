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

    # start a Solr so we can use the Schema API, but only on localhost
    echo "starting local solr"
    start-local-solr &
    echo "waiting 10 seconds for solr to start"
    sleep 10

    # Now configure with the Schema API

    echo "adding /update/extract request handler"
    curl -s --fail-with-body -X POST -H 'Content-type:application/json' -d '{
      "add-requesthandler": {
        "name": "/update/extract",
        "startup": "lazy",
        "class": "solr.extraction.ExtractingRequestHandler",
        "defaults":{ "lowernames": "true", "fmap.content":"_text_"}
      }
    }' http://$solr_host/solr/$core_name/config || exit 1

    echo "adding /LearnwebQuery request handler"
    curl -s --fail-with-body --fail -X POST -H 'Content-type:application/json' -d '{
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
    }' http://$solr_host/solr/$core_name/config || exit 1

    echo "adding schema fields"
    curl -s --fail-with-body --fail -X POST -H 'Content-type:application/json' --data-binary '{
  "add-field":[
    {
      "name": "title",
      "type": "text_general",
      "multiValued": false
    },
    {
      "name": "author",
      "type": "text_general",
      "multiValued": false
    },
    {
      "name": "description",
      "type": "text_general",
      "multiValued": false
    },
    {
      "name": "machineDescription",
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
      "name": "timestamp",
      "type": "pdate"
    },
    {
      "name": "type",
      "type": "string"
    },
    {
      "name": "url",
      "type": "string"
    },
    {
      "name": "tags",
      "type": "text_general"
    },
    {
      "name": "comments",
      "type": "text_general"
    },
    {
      "name": "fulltext",
      "type": "text_general",
      "multiValued": true,
      "indexed": true,
      "stored": false
    }
  ],
  "add-copy-field":[
    {
      "source": "author",
      "dest": ["fulltext", "author_s"]
    },
    {
      "source": "comments",
      "dest": "fulltext"
    },
    {
      "source": "description",
      "dest": "fulltext"
    },
    {
      "source": "machineDescription",
      "dest": "fulltext"
    },
    {
      "source": "tags",
      "dest": "fulltext"
    },
    {
      "source": "title",
      "dest": "fulltext"
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
   }' http://$solr_host/solr/$core_name/schema || exit 1

    echo "finished configuring with the Schema API"

    stop-local-solr
    sleep 1

    touch $sentinel
fi
