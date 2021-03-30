import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.parser.NxParser;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.stream.Stream;

public class NxParserStream extends NxParser {
    public NxParserStream(InputStream is, URI baseURI) {
        super(is, baseURI);
    }
    
    public NxParserStream(InputStream is) {
        super(is);
    }
    
    public NxParserStream(InputStream is, Charset cs, URI baseURI) {
        super(is, cs, baseURI);
    }
    
    public NxParserStream(InputStream is, Charset cs) {
        super(is, cs);
    }
    
    public NxParserStream(Reader r, URI baseURI) {
        super(r, baseURI);
    }
    
    public NxParserStream(Reader r) {
        super(r);
    }
    
    public NxParserStream(Iterable<String> it) {
        super(it);
    }
    
    public NxParserStream(Iterator<String> it) {
        super(it);
    }
}
