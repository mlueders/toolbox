package org.roadkill.xml

class XmlNodeStringTranslator {
    
    private XmlParser parser = new XmlParser()
    private ByteArrayOutputStream stream = new ByteArrayOutputStream()
    private PrintWriter writer = new PrintWriter(stream)
    private XmlNodePrinter printer = new XmlNodePrinter(writer)
    
    String nodeToText(Node node) {
        printer.print(node)
        writer.flush()
        String nodeAsString = stream.toString()
        stream.reset()
        nodeAsString
    }
    
    Node textToNode(String textAsString) {
        parser.parseText(textAsString)
    }

    void writeNodeToFile(Node node, File file) {
        file.parentFile.mkdirs()

        file.withWriter { BufferedWriter writer ->
            writer.writeLine('<?xml version="1.0" encoding="UTF-8"?>')
            writer << nodeToText(node)
        }
    }

    Node getFileAsNodeIfExists(File file) {
        Node root = null

        if (file.exists()) {
            root = textToNode(file.text)
        }
        root
    }

}