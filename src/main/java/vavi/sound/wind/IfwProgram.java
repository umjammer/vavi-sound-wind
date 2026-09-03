/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.wind;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import static java.lang.System.getLogger;


/**
 * IfwProgram. one IFW tone, the unit a {@code .xml} file under {@code ~/Documents/IFW/Sounds} holds.
 * <pre>
 * &lt;IFWState&gt;
 *   &lt;CurrentProgram&gt;
 *     &lt;IFWProgram&gt;
 *       &lt;Name&gt;Pan Flute&lt;/Name&gt;
 *       &lt;Author/&gt;&lt;Comment/&gt;
 *       &lt;Parameters&gt;&lt;Osc1Sync&gt;Off&lt;/Osc1Sync&gt; ... &lt;/Parameters&gt;
 * </pre>
 * every parameter is optional: a file written by an older IFW stops partway through the
 * table and the slots it never wrote keep {@link IfwParameter#defaultValue()}.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-04 nsano initial version <br>
 * @see IfwParameter
 */
public class IfwProgram {

    private static final Logger logger = getLogger(IfwProgram.class.getName());

    /** the most a tone file can be, it is a couple of hundred knob positions */
    private static final int MAX_SIZE = 1 << 20;

    /** enough of the head of a stream to recognize a tone file by */
    private static final int HEAD = 512;

    /** an ampersand that does not start an entity reference, which IFW writes into names */
    private static final Pattern BARE_AMPERSAND =
            Pattern.compile("&(?!#[0-9]+;|#x[0-9a-fA-F]+;|[a-zA-Z][a-zA-Z0-9]*;)");

    /** */
    private String name = "Initial Program";

    /** */
    private String author = "";

    /** */
    private String comment = "";

    /** indexed by {@link IfwParameter#ordinal()} */
    private final float[] values = new float[IfwParameter.values().length];

    /** an IFW initial program */
    public IfwProgram() {
        for (IfwParameter p : IfwParameter.values()) {
            values[p.ordinal()] = p.defaultValue();
        }
    }

    /** */
    public IfwProgram(IfwProgram that) {
        this.name = that.name;
        this.author = that.author;
        this.comment = that.comment;
        System.arraycopy(that.values, 0, this.values, 0, values.length);
    }

    /** */
    public String getName() {
        return name;
    }

    /** */
    public void setName(String name) {
        this.name = name;
    }

    /** */
    public String getAuthor() {
        return author;
    }

    /** */
    public void setAuthor(String author) {
        this.author = author;
    }

    /** */
    public String getComment() {
        return comment;
    }

    /** */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /** */
    public float get(IfwParameter parameter) {
        return values[parameter.ordinal()];
    }

    /** */
    public void set(IfwParameter parameter, float value) {
        values[parameter.ordinal()] = parameter.clamp(value);
    }

    /** the constant a selector parameter currently points at */
    public <E extends Enum<E> & Labeled> E choice(IfwParameter parameter, Class<E> type) {
        E[] constants = type.getEnumConstants();
        return constants[Math.min(constants.length - 1, Math.max(0, Math.round(get(parameter))))];
    }

    /** */
    public void set(IfwParameter parameter, Enum<?> choice) {
        set(parameter, choice.ordinal());
    }

    /** whether a switch parameter is on */
    public boolean is(IfwParameter parameter) {
        return choice(parameter, Bool.class).isOn();
    }

    /** the parameters this program differs from the IFW initial program in */
    public Map<IfwParameter, Float> diff() {
        Map<IfwParameter, Float> map = new EnumMap<>(IfwParameter.class);
        for (IfwParameter p : IfwParameter.values()) {
            if (values[p.ordinal()] != p.defaultValue()) {
                map.put(p, values[p.ordinal()]);
            }
        }
        return map;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IfwProgram that && name.equals(that.name) && Arrays.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return name.hashCode() * 31 + Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        return "IfwProgram[" + name + "]";
    }

    /**
     * Reads a tone file.
     *
     * @throws IOException the stream is not an IFW tone file
     */
    public static IfwProgram read(InputStream in) throws IOException {
        Element program = programElement(in);
        IfwProgram result = new IfwProgram();
        result.name = text(program, "Name", result.name);
        result.author = text(program, "Author", "");
        result.comment = text(program, "Comment", "");

        Element parameters = child(program, "Parameters");
        if (parameters != null) {
            NodeList children = parameters.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (!(children.item(i) instanceof Element e)) {
                    continue;
                }
                IfwParameter p = IfwParameter.valueOfId(e.getTagName());
                if (p == null) {
                    // a parameter of an IFW newer than this table
logger.log(Level.DEBUG, "unknown parameter, ignored: " + e.getTagName());
                    continue;
                }
                try {
                    result.values[p.ordinal()] = p.parse(e.getTextContent());
                } catch (IllegalArgumentException ex) {
                    // a label of an IFW newer than this table, keep the default
logger.log(Level.DEBUG, "unreadable value, defaulted: " + e.getTagName() + ": " + ex.getMessage());
                }
            }
        }
        return result;
    }

    /** Writes a tone file the plug-in can load back. */
    public void write(OutputStream out) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        writer.write("<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n");
        writer.write("<IFWState>\n");
        writer.write("    <CurrentProgram>\n");
        writer.write("        <IFWProgram>\n");
        writer.write("            <Name>" + escape(name) + "</Name>\n");
        writer.write("            <Author>" + escape(author) + "</Author>\n");
        writer.write("            <Comment>" + escape(comment) + "</Comment>\n");
        writer.write("            <Parameters>\n");
        for (IfwParameter p : IfwParameter.values()) {
            String value = escape(p.format(values[p.ordinal()]));
            writer.write("                <" + p.id() + ">" + value + "</" + p.id() + ">\n");
        }
        writer.write("            </Parameters>\n");
        writer.write("        </IFWProgram>\n");
        writer.write("    </CurrentProgram>\n");
        writer.write("</IFWState>\n");
        writer.flush();
    }

    /**
     * Whether the stream starts an IFW tone file.
     * <p>
     * the stream must support mark, and is left where it was found.
     */
    public static boolean isIfw(InputStream in) throws IOException {
        in.mark(HEAD);
        try {
            String head = new String(in.readNBytes(HEAD), StandardCharsets.UTF_8);
            return head.contains("<IFWState");
        } finally {
            in.reset();
        }
    }

    /** */
    private static Element programElement(InputStream in) throws IOException {
        try {
            String text = readAll(in);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // a tone file is plain data, it has no reason to reach out
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            builder.setErrorHandler(null);
            Document document = builder.parse(new InputSource(new StringReader(text)));
            Element state = document.getDocumentElement();
            if (state == null || !"IFWState".equals(state.getTagName())) {
                throw new IOException("not an IFW tone file: " + (state == null ? null : state.getTagName()));
            }
            // usually IFWState/CurrentProgram/IFWProgram, but hand edited tones drop the wrapper
            NodeList programs = state.getElementsByTagName("IFWProgram");
            if (programs.getLength() == 0) {
                throw new IOException("no IFWProgram in the IFW tone file");
            }
            return (Element) programs.item(0);
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    /**
     * Reads a whole tone file and patches up what IFW writes that XML does not allow.
     * <p>
     * IFW pads the file with a NUL after the root element, and it writes the name, the
     * author and the comment straight out, so a tone called {@code Unaji&Travel} carries
     * a bare ampersand. neither survives a parser, and both are what the plug-in itself
     * reads back, so a reader that wants to open a player's own tones has to cope.
     */
    private static String readAll(InputStream in) throws IOException {
        String text = new String(in.readNBytes(MAX_SIZE), StandardCharsets.UTF_8);
        int end = text.length();
        while (end > 0 && (text.charAt(end - 1) == 0 || Character.isWhitespace(text.charAt(end - 1)))) {
            end--;
        }
        int start = end > 0 && text.charAt(0) == '\ufeff' ? 1 : 0;
        if (end <= start) {
            throw new IOException("empty stream");
        }
        return BARE_AMPERSAND.matcher(text.substring(start, end)).replaceAll("&amp;");
    }

    /** */
    private static Element child(Element parent, String tag) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node instanceof Element e && e.getTagName().equals(tag)) {
                return e;
            }
        }
        return null;
    }

    /** */
    private static String text(Element parent, String tag, String defaultValue) {
        Element e = child(parent, tag);
        return e == null ? defaultValue : e.getTextContent().trim();
    }

    /** */
    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
