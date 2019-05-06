package cz.cvut.kbss.termit.service.document.html;

import cz.cvut.kbss.termit.model.selector.TermSelector;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

/**
 * Generator of HTML/XML selectors.
 */
@FunctionalInterface
interface SelectorGenerator {

    /**
     * Generates selector for the specified elements' content.
     * <p>
     * The reason multiple elements are supported is because in case there are overlapping annotations, they are
     * represented by multiple elements using the <a href="https://en.wikipedia.org/wiki/Overlapping_markup#Joins">JOINS</a>
     * strategy.
     *
     * @param elements Elements to generate selector for. At least one must be provided
     * @return Selector for the text content of the specified elements
     */
    TermSelector generateSelector(Element... elements);

    /**
     * Extracts text content of the specified elements, joining them into one string.
     *
     * @param elements Elements to extract text from
     * @return Text content
     */
    default String extractExactText(Element[] elements) {
        final StringBuilder sb = new StringBuilder();
        for (Element element : elements) {
            sb.append(element.wholeText());
        }
        return sb.toString();
    }

    default StringBuilder extractNodeText(Iterable<Node> nodes) {
        final StringBuilder sb = new StringBuilder();
        for (Node node : nodes) {
            if (node instanceof Comment) {
                continue;
            }
            sb.append(node instanceof TextNode ? ((TextNode) node).getWholeText() : ((Element) node).wholeText());
        }
        return sb;
    }
}
