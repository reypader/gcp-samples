import logging

import base64
import json

import sys
from flask import Flask, request
from google.appengine.api import search
from google.appengine.api.search import Query, QueryOptions
from google.appengine.ext import ndb
from flask import jsonify
import traceback

app = Flask(__name__)


class Product(ndb.Model):
    name = ndb.StringProperty()
    url = ndb.StringProperty()
    image = ndb.StringProperty()

    def serialize(self):
        return {
            'name': self.name,
            'url': self.url,
            'image': self.image,
        }


@app.route('/search')
def search_product():
    try:
        search_string = request.args.get('q')
        result = query_documents(search_string)
        logging.info("Document query result: {}".format(str(result)))
        resp = find(result)
        logging.info("Datastore query result: {}".format(str(resp)))
        return jsonify(result=[r.serialize() for r in resp])
    except:
        print "Error! ", sys.exc_info()[0]
        traceback.print_exc()


def find(result):
    keys = [ndb.Key(Product, int(x.doc_id)) for x in result]
    return ndb.get_multi(keys)


def query_documents(q):
    index = search.Index('products')
    query_string = 'product_name: {}'.format(q)
    return index.search(
        query=Query(query_string,
                    options=QueryOptions(limit=20)))


@app.route('/_ah/push-handlers/add_product', methods=['POST'])
def add_product():
    try:
        envelope = json.loads(request.data.decode('utf-8'))
        payload = base64.b64decode(envelope['message']['data'])
        logging.info(payload)
        p = json.loads(payload)
        create_document(p)
        persist(p)
    except:
        print "Error! ", sys.exc_info()[0]
        traceback.print_exc()
    return 'OK', 200


def persist(payload):
    product = Product(id=payload['sku'], name=payload['name'], url=payload['url'], image=payload['image'])
    product.put()


def create_document(payload):
    name = ','.join(tokenize_autocomplete(payload['name']))
    document = search.Document(
        doc_id=str(payload['sku']),
        fields=[
            search.TextField(name='product_name', value=name)
        ])
    index = search.Index('products')
    index.put(document)
    logging.info("Document creation done")
    return document


def tokenize_autocomplete(phrase):
    a = []
    for x in phrase.split():
        for word in x.split("-"):
            for i in range(len(word) + 1):
                w = word[:i]
                if w and w != '-':
                    a.append(w)

    return a
