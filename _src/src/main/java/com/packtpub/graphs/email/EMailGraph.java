/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.packtpub.graphs.email;

import com.sun.mail.imap.IMAPFolder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneId;
import static java.time.temporal.ChronoUnit.DAYS;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.search.ComparisonTerm;
import javax.mail.search.ReceivedDateTerm;
import javax.mail.search.SearchTerm;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.io.IoCore;
import org.apache.tinkerpop.gremlin.tinkergraph.structure.TinkerGraph;

/**
 *
 * @author Erik Costlow
 */
public class EMailGraph {

    private static final Logger LOG = Logger.getLogger(EMailGraph.class.getSimpleName());

    public static void main(String[] args) {
        final EMailGraph self = new EMailGraph();
        self.go();
    }

    private Vertex me;

    // For GMail, you may need https://support.google.com/mail/answer/7126229?hl=en
    // For GMail multifactor auth (recommended), use an app password
    private void go() {
        
        try (InputStream in = Files.newInputStream(Paths.get("emailCredentials.properties"))) {
            final Properties properties = new Properties();
            properties.load(in);
            final String email = properties.getProperty("username");
            final String passwd = properties.getProperty("password");
            final String host = properties.getProperty("host");

            parse(email, passwd, host);
        } catch (IOException ex) {
            Logger.getLogger(EMailGraph.class.getName()).log(Level.SEVERE, "Unable to load email credentials", ex);
        }

        System.out.println("Done, see https://www.slideshare.net/gephi/gephi-quick-start for how to open the graphml");
    }

    private void parse(String email, String password, String host) {
        Session session = Session.getInstance(new Properties(), null);
        session.setDebug(false);
        final String url = String.format("imaps://%s:%s@%s", email, password, host);
        URLName urln = new URLName(url);

        try {
            connect(session, urln);
        } catch (MessagingException ex) {
            Logger.getLogger(EMailGraph.class.getName()).log(Level.SEVERE, "Unable to connect", ex);
        }
    }

    private void connect(Session session, URLName urln) throws MessagingException {
        try (Store store = session.getStore(urln)) {
            store.connect();

            System.out.println("Default Folders:");
            Arrays.stream(store.getPersonalNamespaces()).forEach(folder -> System.out.println("  " + folder.getFullName()));
            Arrays.stream(store.getSharedNamespaces()).forEach(folder -> System.out.println("  " + folder.getFullName()));
            final Folder root = store.getFolder("Inbox");
            System.out.println("Inbox Folders:");
            Arrays.stream(root.list()).forEach(folder -> System.out.println("  " + folder.getFullName()));
            graph(root);
        }

    }

    private void graph(Folder folder) throws MessagingException {
        if (folder instanceof IMAPFolder) {
            System.out.println("Folder is " + folder.getName());
            try (final IMAPFolder f = (IMAPFolder) folder;
                    Graph graph = TinkerGraph.open();) {
                me = graph.addVertex("name", "me");
                f.open(Folder.READ_ONLY);

                final Date since = Date.from(LocalDate.now().minus(90, DAYS).atStartOfDay(ZoneId.systemDefault()).toInstant());
                final SearchTerm newer = new ReceivedDateTerm(ComparisonTerm.GT, since);

                final Message[] messages = folder.search(newer);

                Arrays.stream(messages).forEach(message -> perMessage(graph, message));

                graph.io(IoCore.graphml()).writeGraph("email.graphml");
            } catch (Exception e) {
                LOG.severe("Unable to graph email: " + e.getMessage());
            }
        } else {
            System.err.println("Ignoring non-IMAP, don't want to impact actual email.");
        }
    }

    private void perMessage(Graph graph, Message message) {
        try {
            final Address[] from = message.getFrom();

            final String joinedFrom = Arrays.stream(message.getFrom()).map(EMailGraph::formatEmail).collect(Collectors.joining(", "));
            final Vertex fromVertex = findOrCreate(graph, joinedFrom);
            plot(fromVertex, me);

            final Address[] ccPeople = message.getRecipients(Message.RecipientType.CC);
            if (ccPeople != null) {
                Arrays.stream(ccPeople).forEach(ccMember -> {
                    final String joinedCC = formatEmail(ccMember);
                    final Vertex ccVertex = findOrCreate(graph, joinedCC);
                    plot(fromVertex, ccVertex);
                });
            }

            System.out.println("Email subject " + message.getSubject() + " ::: is from " + joinedFrom);
        } catch (MessagingException ex) {
            Logger.getLogger(EMailGraph.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static String formatEmail(Address address) {
        //A nice way to prune, I want the actual name not the address.
        final int index = String.valueOf(address).indexOf('<');
        if (index > 0) {
            return String.valueOf(address).substring(0, index - 1);
        }
        return String.valueOf(address);
    }

    private Vertex findOrCreate(Graph graph, String name) {
        final GraphTraversal<Vertex, Vertex> gt = graph.traversal().V().has("name", name);
        final Vertex v;
        if (gt.hasNext()) {
            v = gt.next();
        } else {
            v = graph.addVertex("name", name);
        }
        return v;

    }

    private void plot(Vertex from, Vertex to) {

        final Iterator<Edge> edges = from.edges(Direction.OUT, "contactedBy");
        AtomicBoolean foundEdge = new AtomicBoolean(false);
        edges.forEachRemaining(edge -> {
            if (edge.inVertex().id().equals(to.id())) {
                final Property prop = edge.property("count");
                prop.ifPresent(obj -> edge.property("count", ((Integer) obj) + 1));
                foundEdge.set(true);
            }
        });
        if (!foundEdge.get()) {
            from.addEdge("contactedBy", to, "count", 1);
        }

    }
}
