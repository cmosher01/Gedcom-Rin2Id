package nu.mine.mosher.gedcom;

import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.exception.InvalidLevel;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * For each top-level record (INDI, FAM, SOUR, etc.) that has a RIN, replace the
 * record's ID with the value of its RIN.
 *
 * Created by user on 12/10/16.
 */
public class GedcomRin2Id {
    private final File file;
    private Charset charset;
    private GedcomTree gt;
    private final Map<String, String> mapRemapIds = new HashMap<>(4096);



    public static void main(final String... args) throws InvalidLevel, IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("usage: java -jar gedcom-rin2id in.ged >out.ged");
        } else {
            new GedcomRin2Id(args[0]).main();
        }
    }



    GedcomRin2Id(final String filename) {
        this.file = new File(filename);
    }

    public void main() throws IOException, InvalidLevel {
        loadGedcom();
        updateGedcom();
        saveGedcom();
    }

    private void loadGedcom() throws IOException, InvalidLevel {
        this.charset = Gedcom.getCharset(this.file);
        this.gt = Gedcom.parseFile(file, this.charset, false);
    }

    private void updateGedcom() {
        buildIdMap(this.gt.getRoot());
        remapIds(this.gt.getRoot());
    }

    private void saveGedcom() throws IOException {
        final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FileDescriptor.out), this.charset));

        Gedcom.writeFile(this.gt, out, Integer.MAX_VALUE);

        out.flush();
        out.close();
    }



    private void buildIdMap(final TreeNode<GedcomLine> root) {
        for (final TreeNode<GedcomLine> top : root) {
            final GedcomLine gedcomLine = top.getObject();
            if (gedcomLine != null) {
                if (gedcomLine.hasID()) {
                    final String idOld = gedcomLine.getID();
                    final String idNew = findRin(top);
                    if (!idNew.isEmpty()) {
                        this.mapRemapIds.put(idOld, idNew);
                    }
                }
            }
        }
    }

    private static String findRin(final TreeNode<GedcomLine> top) {
        for (final TreeNode<GedcomLine> attr : top) {
            final GedcomLine gedcomLine = attr.getObject();
            if (gedcomLine != null && gedcomLine.getTag().equals(GedcomTag.RIN)) {
                return gedcomLine.getValue();
            }
        }
        return "";
    }

    private void remapIds(final TreeNode<GedcomLine> node) {
        node.forEach(c -> remapIds(c));

        final GedcomLine gedcomLine = node.getObject();
        if (gedcomLine != null) {
            if (gedcomLine.hasID()) {
                final String idNew = this.mapRemapIds.get(gedcomLine.getID());
                if (idNew != null) {
                    node.setObject(new GedcomLine(gedcomLine.getLevel(), "@"+idNew+"@", gedcomLine.getTagString(), gedcomLine.getValue()));
                }
            }
            if (gedcomLine.isPointer()) {
                final String idNew = this.mapRemapIds.get(gedcomLine.getPointer());
                if (idNew != null) {
                    node.setObject(new GedcomLine(gedcomLine.getLevel(), "", gedcomLine.getTagString(), "@"+idNew+"@"));
                }
            }
        }
    }
}
