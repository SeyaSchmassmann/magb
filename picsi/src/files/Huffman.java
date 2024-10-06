package files;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.BitSet;
import java.util.PriorityQueue;

import javax.swing.JTextArea;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

import imageprocessing.ImageProcessing;

/**
 * Huffman file format and codec
 * @author Christoph Stamm
 *
 */
public class Huffman implements IImageFile {
	static class Node implements Comparable<Node>, Serializable {
		static final long serialVersionUID = 1;
		protected transient double m_p; 	// probability (not needed during decoding)
		protected transient long m_code;	// binary code; maximum 64 bits  (not needed during decoding)
		protected transient byte m_codeLen;	// binary code length (not needed during decoding)
		protected Node m_left, m_right;		// children

		public Node(double p) {
			m_p = p;
		}
		
		public Node(Node left, Node right) {
			m_left = left;
			m_right = right;
			m_p = left.m_p + right.m_p;
		}

		public double getProbability() {
			return m_p;
		}

		public long getCode() {
			return m_code;
		}

		public byte getCodeLen() {
			return m_codeLen;
		}

		public boolean isInnerNode() {
			return m_left != null;
		}
		
		public int compareTo(Node v) {
			if (v != null) {
				if (m_p < v.m_p) {
					return -1;
				} else if (m_p == v.m_p) {
					return 0;
				} else {
					return 1;
				}
			} else {
				return -1;
			}
		}

		public void setCode(long code, int codeLen) {
			m_code = code;
			m_codeLen = (byte)codeLen;

			if (m_left != null) {
				// 0-Bit
				m_left.setCode(code << 1, codeLen + 1);				
			}
			if (m_right != null) {
				// 1-Bit
				m_right.setCode((code << 1) + 1, codeLen + 1);
			}
		}
		
		public Node decodeBit(boolean bit) {
			return (bit) ? m_right : m_left;
		}
	}

	static class Leaf extends Node {
		static final long serialVersionUID = 1;
		private byte m_intensity;	// pixel intensity used during decoding
		
		public Leaf(double p, byte intensity) {
			super(p);
			m_intensity = intensity;
		}

		public byte getIntensity() {
			return m_intensity;
		}
	}

	@Override
	public ImageData read(String fileName) throws Exception {
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));

		try {
			// read Header
			int width = in.readInt();
			int stride = ((width + 3)/4)*4;
			int height = in.readInt();
			
			// read code tree
			Node root = (Node)in.readObject();
	
			// read compressed data
			BitSet data = (BitSet)in.readObject();
	
			// close file
			in.close();
			
			// create palette
			RGB[] palette = new RGB[256];
			for (int i=0; i < palette.length; i++) {
				palette[i] = new RGB(i, i ,i);
			}
			
			// create image
			byte[] raw = new byte[stride*height];
			ImageData inData = new ImageData(width, height, 8, new PaletteData(palette), 4, raw); // stride is a multiple of 4 bytes
	
			// fill in image data
			Node node;
			int index = 0;
			
			for(int v = 0; v < inData.height; v++) {
				for(int u = 0; u < inData.width; u++) {
					node = root;
					while(node.isInnerNode()) {
						node = node.decodeBit(data.get(index++));
					}
					inData.setPixel(u, v, ((Leaf)node).getIntensity());
				}
			}
			return inData;	
		} finally {
			in.close();
		}
}

	@Override
	public void save(String fileName, int fileType, ImageData imageData, int imageType) throws Exception {
		final int w = imageData.width;
		final int h = imageData.height;
		final int size = w*h;
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));

		try {
			// huffman encoding
			int[] hist = ImageProcessing.histogram(imageData, 256);
			Leaf[] codes = new Leaf[hist.length];
			Node root = createHuffmanTree(hist, codes, size);
			BitSet data = encodeImage(imageData, codes);

			
			// TODO Header abspeichern
            out.writeInt(w);
            out.writeInt(h);
            
			// TODO Codebaum abspeichern
            out.writeObject(root);

			// TODO komprimierte Bilddaten abspeichern
            out.writeObject(data);

		} finally {
			out.close();
		}
	}
	
	/**
	 * Build code tree
	 * @param hist histogram of input image
	 * @param codes code table
	 * @param size number of pixels
	 * @return root node of code tree
	 */
	private Node createHuffmanTree(int[] hist, Leaf[] codes, int size) {
		PriorityQueue<Node> pq = new PriorityQueue<Node>(hist.length);

		// TODO Wahrscheinlichkeiten und Entropie berechnen und neue Blattknoten erzeugen (die Blattknoten sowohl in die Code-Tabelle als auch in die PQ einfügen)

        // var entropy = 0.0;
        for (int i = 0; i < hist.length; i++) {
            var histEntry = hist[i];
            var p = (double) histEntry / size;
            var leaf = new Leaf(p, (byte) i);
            codes[i] = leaf;
            pq.add(leaf);
            // var histEntry = hist[i];
            // if (histEntry > 0) {
            //     entropy -= p * Math.log(p) / Math.log(2);
            // 
            //     if (p > 0) {
            //         System.out.println("Intensity: " + histEntry + ", Probability: " + p);
            //     }
            // }
        }

		// TODO Mittlere Codel�nge und Datei-Speicherbedarf mithilfe der Entropie absch�tzen (Unter- und Obergrenze) und auf der Konsole ausgeben

        // var lowerBound = entropy;
        // var upperBound = entropy + 1;

        // System.out.println("Lower bound: " + lowerBound);
        // System.out.println("Upper bound: " + upperBound);

		// TODO Codebaum aufbauen: Verwenden Sie die pq, um die zwei jeweils kleinsten Nodes zu holen

        // Node root = null;
        while (pq.size() > 1) {
            var left = pq.poll();
            var right = pq.poll();
            //root = new Node(left, right);
            //pq.add(root);
            pq.add(new Node(left, right));
        }

        var root = pq.poll();
        assert pq.isEmpty();
        
		// TODO Wurzelknoten holen und mittels Funktionsaufruf alle Codes rekursiv erzeugen

        root.setCode(0, 0);
        //defineCode(root, 0, 0, codes);

		// TODO Mittlere Codel�nge und Datei-Speicherbedarf berechnen und auf der Konsole ausgeben

        // var avgCodeLen = 0.0;
        // var storage = 0.0;
        // for (int i = 0; i < hist.length; i++) {
        //     var histEntry = hist[i];
        //     if (histEntry > 0) {
        //         var p = (double) histEntry / size;
        //         var codeLen = codes[i].getCodeLen();
        //         avgCodeLen += p * codeLen;
        //         storage += p * codeLen;
        //     }
        // }

        // System.out.println("Average code length: " + avgCodeLen);
        // System.out.println("Storage: " + storage);

		return root;
	}

    private static void defineCode(Node node, long code, int codeLen, Leaf[] codes) {
        if (node instanceof Leaf) {
            var leaf = (Leaf) node;
            leaf.setCode(code, codeLen + 1);
            codes[leaf.getIntensity() & 0xFF] = leaf;
        } else {
            var innerNode = (Node) node;
            defineCode(innerNode.m_left, code << 1, codeLen + 1, codes);
            defineCode(innerNode.m_right, (code << 1) + 1, codeLen + 1, codes);
        }
    }

	/**
	 * Encode image data
	 * @param outData image data
	 * @param codes code table
	 * @return encoded data
	 */
	private BitSet encodeImage(ImageData outData, Leaf[] codes) {
		final int w = outData.width;
		final int h = outData.height;
		BitSet bs = new BitSet();

		// TODO alle Pixel der Reihe nach codieren und im bs abspeichern

        var index = 0;
        for (var y = 0; y < h; y++) {
            for (var x = 0; x < w; x++) {
                var intensity = outData.getPixel(x, y);
                var code = codes[intensity].getCode();
                var codeLen = codes[intensity].getCodeLen();
                for (var i = 1; i <= codeLen; i++) {
                    bs.set(index++, (code & (1 << (codeLen - i))) > 0);
                }
            }
        }
		
		return bs;
	}
	
	@Override
	public void displayTextOfBinaryImage(ImageData imageData, JTextArea text) {
		text.append("P2");
		text.append("\n" + imageData.width + " " + imageData.height);
		text.append("\n255\n");
		PNM.writePGM(imageData, text, 255);
	}

	@Override
	public boolean isBinaryFormat() {
		return true;
	}

}
