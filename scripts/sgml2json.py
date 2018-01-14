# -*- coding:utf-8 -*-
import os
from sgmllib import SGMLParser
import json
import codecs


class SGMLDocParser(SGMLParser):

    def reset(self):
        self._id = ''
        self._type = ''
        self.getDataFlag = False
        self.docs = []
        SGMLParser.reset(self)

    def start_doc(self, attrs):
        for k, v in attrs:
            if k == 'id':
                self._id = v
            if k == 'type':
                self._type = v
        self.docs.append({'id': self._id, 'type': self._type, 'paragraphs': []})

    def end_doc(self):
        self._id = ''
        self._type = ''

    def start_p(self, attrs):
        self.getDataFlag = True

    def end_p(self):
        self.getDataFlag = False

    def handle_data(self, text):
        text = text.replace('\n', ' ')
        if self.getDataFlag:
            self.docs[-1]['paragraphs'].append(text)

    def dumpJson(self, filename):
        with codecs.open(filename, 'w', encoding='utf-8') as outputFile:
            json.dump(self.docs, outputFile)
        print('Finish {}'.format(filename))


if __name__ == '__main__':
    path = '../data/'
    for filename in os.listdir(path):
        parser = SGMLDocParser(path + filename)
        with codecs.open(path + filename, 'r', encoding='utf-8') as inputFile:
            parser.feed(inputFile.read())
        parser.dumpJson(path + filename + '.json')
