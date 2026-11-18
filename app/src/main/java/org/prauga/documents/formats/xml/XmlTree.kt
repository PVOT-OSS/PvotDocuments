package org.prauga.documents.formats.xml

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream

/** [input] parsed into a tree. Namespace prefixes are kept as written ("a:p", "draw:frame"). */
internal fun parseXmlTree(input: InputStream): XmlNode {
    val p = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        setInput(input, null)
    }
    val root = XmlNode("#document", emptyMap())
    val stack = ArrayList<XmlNode>().apply { add(root) }
    while (true) {
        when (p.next()) {
            XmlPullParser.START_TAG -> {
                val attrs = HashMap<String, String>(p.attributeCount * 2)
                for (i in 0 until p.attributeCount) attrs[p.getAttributeName(i)] = p.getAttributeValue(i)
                val node = XmlNode(p.name, attrs)
                stack.last().children.add(node)
                stack.add(node)
            }
            XmlPullParser.END_TAG -> stack.removeAt(stack.lastIndex)
            XmlPullParser.TEXT -> {
                val text = p.text
                if (text.isNotEmpty()) stack.last().children.add(XmlNode.text(text))
            }
            XmlPullParser.END_DOCUMENT -> break
        }
    }
    return root.children.firstOrNull { it.name != XmlNode.TEXT } ?: root
}
