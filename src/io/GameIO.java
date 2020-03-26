package io;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import gameObjects.definition.GameObject;
import gameObjects.definition.GameObjectCard;
import gameObjects.definition.GameObjectCard.CardState;
import gameObjects.instance.Game;
import gameObjects.instance.GameInstance;
import gameObjects.instance.ObjectInstance;
import gameObjects.instance.ObjectState;
import main.Player;

public class GameIO {
	public Player readPlayer(ZipInputStream stream)
	{
		return null;
	}
	
	private static void exportState(ObjectState state, Element elem)
	{
		elem.setAttribute("x", Integer.toString(state.posX));
		elem.setAttribute("y", Integer.toString(state.posY));
		elem.setAttribute("r", Integer.toString(state.rotation));
		elem.setAttribute("owner", Integer.toString(state.owner_id));
		elem.setAttribute("above", Integer.toString(state.aboveInstanceId));
		elem.setAttribute("below", Integer.toString(state.belowInstanceId));
		if (state instanceof CardState)
    	{
			elem.setAttribute("side", Boolean.toString(((CardState)state).side));
    	}
	}
	
	private static void importState(ObjectState state, Element elem)
	{
		state.posX = Integer.parseInt(elem.getAttributeValue("x"));
		state.posY = Integer.parseInt(elem.getAttributeValue("y"));
		String v = elem.getAttributeValue("above");
		if (v != null)
		{
			state.aboveInstanceId = Integer.parseInt(v);
		}
		v = elem.getAttributeValue("below");
		if (v != null)
		{
			state.belowInstanceId = Integer.parseInt(v);
		}
		state.rotation = Integer.parseInt(elem.getAttributeValue("r"));
		Attribute ownerAttribute = elem.getAttribute("owner_id");
		if (ownerAttribute != null)
		{
	        state.owner_id = Integer.parseInt(ownerAttribute.getValue());
		}
		if (state instanceof CardState && elem.getAttribute("side") != null)
    	{
			((CardState)state).side = Boolean.parseBoolean(elem.getAttributeValue("side"));
    	}
	}

	
	public static void writeSnapshotToZip(GameInstance gi, OutputStream os) throws IOException
	{
		ZipOutputStream zipOutputStream = null;
		try
		{
			zipOutputStream = new ZipOutputStream(os);
			Game game = gi.game;
			// Save all images
		    Iterator<Entry<String, BufferedImage>> it = game.images.entrySet().iterator();
		    while (it.hasNext()) {
		    	HashMap.Entry<String, BufferedImage> pair = it.next();
		    	String key = pair.getKey();
			    ZipEntry imageZipOutput = new ZipEntry(key);
			    zipOutputStream.putNextEntry(imageZipOutput);

			    if (key.endsWith(".jpg"))
			    {
			    	ImageIO.write(pair.getValue(), "jpg", zipOutputStream);
			    }
			    else if (key.endsWith(".png"))
			    {
			    	ImageIO.write(pair.getValue(), "png", zipOutputStream);
			    }
			    zipOutputStream.closeEntry();
		    }
		    
		    // save game.xml
		    Document doc_game = new Document();
	    	Element root_game = new Element("xml");
	    	doc_game.addContent(root_game);
	    	
	    	Iterator<GameObject> gameIt = game.objects.iterator();
	        while (gameIt.hasNext()) {
	        	GameObject entry = gameIt.next();
	        	Element elem = new Element("object");
	        	if (entry instanceof GameObjectCard)
	        	{
	        		GameObjectCard card = (GameObjectCard) entry;
	        		elem.setAttribute("type", "card");
	        		elem.setAttribute("unique_name", card.uniqueName);
	        		for (String key : game.images.keySet())
	        		{
	        			if(game.images.get(key).equals(card.getUpsideLook())) 
	        			{
	        				elem.setAttribute("front", key);
	        				break;
	        	        }
	        		}
	        		
	        		for (String key : game.images.keySet())
	        		{
	        			if(game.images.get(key).equals(card.getDownsideLook())) 
	        			{
	        				elem.setAttribute("back", key);
	        				break;
	        	        }
	        		}
	        	}
	        	root_game.addContent(elem);
	        }
	        
	        Element elem_back = new Element("background");
	        for (String key : game.images.keySet())
    		{
    			if(game.images.get(key).equals(game.background)) 
    			{
    				elem_back.setText(key);
    				break;
    	        }
    		}
	        root_game.addContent(elem_back);
	    	
	    	ZipEntry gameZipOutput = new ZipEntry("game.xml");
	    	zipOutputStream.putNextEntry(gameZipOutput);
	    	new XMLOutputter(Format.getPrettyFormat()).output(doc_game, zipOutputStream);
	    	zipOutputStream.closeEntry();

		    // save game_instance.xml
	    	Document doc_inst = new Document();
	    	Element root_inst = new Element("xml");
	    	
	    	Iterator<ObjectInstance> instIt = gi.objects.iterator();
	        while (instIt.hasNext()) {
	        	ObjectInstance entry = instIt.next();
	        	Element elem = new Element("object");
        		elem.setAttribute("unique_name", entry.go.uniqueName);
        		elem.setAttribute("id", Integer.toString(entry.id));
        		exportState(entry.state, elem);
        		root_inst.addContent(elem);
        	}
	        Element sessionName = new Element("name");
	        sessionName.setText(gi.name);
	        root_inst.addContent(sessionName);
	    	
	        doc_inst.addContent(root_inst);
	    	ZipEntry xmlZipOutput = new ZipEntry("game_instance.xml");
	    	zipOutputStream.putNextEntry(xmlZipOutput);
	    	new XMLOutputter(Format.getPrettyFormat()).output(doc_inst, zipOutputStream);
	    	zipOutputStream.closeEntry();
		}
		finally
		{
			if (zipOutputStream != null)
			{
				zipOutputStream.close();
			}
		}
	}
	
	public static void writeGameToZip(Game game, OutputStream os) throws IOException
	{	
		ZipOutputStream zipOutputStream = null;
		try
		{
			zipOutputStream = new ZipOutputStream(os);
			
			// Save all images
		    Iterator<Entry<String, BufferedImage>> it = game.images.entrySet().iterator();
		    while (it.hasNext()) {
		    	HashMap.Entry<String, BufferedImage> pair = it.next();
		        //System.out.println(pair.getKey() + " = " + pair.getValue());
		    
			    ZipEntry imageZipOutput = new ZipEntry(pair.getKey());
			    zipOutputStream.putNextEntry(imageZipOutput);

			    if (pair.getKey().endsWith(".jpg"))
			    {
			    	ImageIO.write(pair.getValue(), "jpg", zipOutputStream);
			    }
			    else if (pair.getKey().endsWith(".png"))
			    {
			    	ImageIO.write(pair.getValue(), "png", zipOutputStream);
			    }
			    zipOutputStream.closeEntry();
		    }
		    
			Document doc_game = new Document();
	    	Element root_game = new Element("xml");
	    	doc_game.addContent(root_game);
	    	
	    	Iterator<GameObject> gameIt = game.objects.iterator();
	        while (gameIt.hasNext()) {
	        	GameObject entry = gameIt.next();
	        	Element elem = new Element("object");
	        	if (entry instanceof GameObjectCard)
	        	{
	        		GameObjectCard card = (GameObjectCard) entry;
	        		elem.setAttribute("type", "card");
	        		elem.setAttribute("unique_name", card.uniqueName);
	        		for (String key : game.images.keySet())
	        		{
	        			if(game.images.get(key).equals(card.getUpsideLook())) 
	        			{
	        				elem.setAttribute("front", key);
	        				break;
	        	        }
	        		}
	        		
	        		for (String key : game.images.keySet())
	        		{
	        			if(game.images.get(key).equals(card.getDownsideLook())) 
	        			{
	        				elem.setAttribute("back", key);
	        				break;
	        	        }
	        		}
	        	}
	        	root_game.addContent(elem);
	        }
	        
	        Element elem_back = new Element("background");
	        for (String key : game.images.keySet())
			{
				if(game.images.get(key).equals(game.background)) 
				{
					elem_back.setText(key);
					break;
		        }
			}
	        root_game.addContent(elem_back);
	    	
	        ZipEntry gameZipOutput = new ZipEntry("game.xml");
	    	zipOutputStream.putNextEntry(gameZipOutput);
	    	new XMLOutputter(Format.getPrettyFormat()).output(doc_game, zipOutputStream);
	    	zipOutputStream.closeEntry();
		}
		finally
		{
			if (zipOutputStream != null)
			{
				zipOutputStream.close();
			}
		}
	}

	public static void writeObjectStateToStream(ObjectState object, OutputStream output) throws IOException
	{
		Document doc = new Document();
    	//Element root = new Element("xml");
    	
    	Element elem = new Element("object_state");
		exportState(object, elem);

		//root.addContent(elem);
		doc.addContent(elem);
    	new XMLOutputter(Format.getPrettyFormat()).output(doc, output);

	}

	/* This function is not ready! */
	public static void writeObjectInstanceToStream(ObjectInstance object, OutputStream output) throws IOException
	{
		Document doc = new Document();
    	Element root = new Element("xml");
    	doc.addContent(root);
    	
    	//@Paul: Which info from ObjectState, Player and GameObject are needed?
    	Element elem = new Element("object_instance");
		elem.setAttribute("id", Integer.toString(object.id));
		elem.setAttribute("scale", Double.toString(object.scale));
		elem.setAttribute("width", Integer.toString(object.width));
		elem.setAttribute("height", Integer.toString(object.height));

		new XMLOutputter(Format.getPrettyFormat()).output(doc, output);
	}

	public static GameInstance readSnapshotFromZip(InputStream in) throws IOException, JDOMException
	{
		ZipInputStream stream = new ZipInputStream(in);
		GameInstance result = readSnapshotFromZip(stream);
		in.close();
		return result;
	}

	public static void editGameInstanceFromZip(ZipInputStream stream, GameInstance game, Object source)
	{
		//Editiere nur das was in dem Stream steht
		//rufe dabei die update funktion des games auf, um ﾃｼber die ﾃ､nderungen mitzuteilen
		//Rufe dabei auch die update Methode auf 
	}

	public static void editObjectInstanceFromStream(ObjectInstance objectInstance, InputStream input) throws IOException {
		ZipInputStream zipStream = new ZipInputStream(input);
		editObjectInstanceFromZip(objectInstance, zipStream);
		zipStream.close();
	}

	public static void editObjectInstanceFromZip(ObjectInstance objectInstance, ZipInputStream in) {
		
	}

	public static void editObjectStateFromStream(ObjectState objectState, InputStream input) throws IOException, JDOMException
	{
		Document doc = new SAXBuilder().build(input);
    	Element elem = doc.getRootElement();
    	
    	//for (Element elem : root.getChildren())
    	//{
			importState(objectState, elem);
	   //	}
	}

	public static GameInstance readSnapshotFromZip(ZipInputStream stream) throws IOException, JDOMException
	{
		Game game = new Game();
		HashMap<String, BufferedImage> images = game.images;
		ByteArrayOutputStream gameBuffer = new ByteArrayOutputStream();
		ByteArrayOutputStream gameInstanceBuffer = new ByteArrayOutputStream();
		try
	    {
			ZipEntry entry;
	        while((entry = stream.getNextEntry())!=null)
	        {
	        	String name = entry.getName();
	            if (name.endsWith(".png") || name.endsWith(".jpg"))
	            {
	            	BufferedImage img = ImageIO.read(stream);
	            	//System.out.println("put " + name);
	            	images.put(name, img);
	            }
	            else if (name.equals("game.xml"))
	            {
            	    int nRead;
            	    byte[] data = new byte[1024];
            	    while ((nRead = stream.read(data, 0, data.length)) != -1) {
            	        gameBuffer.write(data, 0, nRead);
            	    }
            	    
	            }
	            else if (name.equals("game_instance.xml"))
	            {
            	    int nRead;
            	    byte[] data = new byte[1024];
            	    while ((nRead = stream.read(data, 0, data.length)) != -1) {
            	        gameInstanceBuffer.write(data, 0, nRead);
            	    }
            	    
	            }
	        }
	    }
	    finally
	    {
	        stream.close();
	    }
		Document doc = new SAXBuilder().build(new ByteArrayInputStream(gameBuffer.toByteArray()));
    	Element root = doc.getRootElement();
    	
    
    	for (Element elem : root.getChildren())
    	{
    		String name = elem.getName();
    		if (name.equals("object"))
    		{
    			switch(elem.getAttributeValue("type"))
    			{
    				case "card":
    				{
    					game.objects.add(new GameObjectCard(elem.getAttributeValue("unique_name"), images.get(elem.getAttributeValue("front")), images.get(elem.getAttributeValue("back"))));
    					break;
    				}
    			}
    		}
    		else if (name.equals("background"))
    		{
    			//System.out.println(elem.getValue());
    			game.background = images.get(elem.getValue());
    		}
    		//System.out.println(name);
	   	}
    	GameInstance result = new GameInstance(game);
    	readGameInstanceFromStream(new ByteArrayInputStream(gameInstanceBuffer.toByteArray()), result);
    	return result;
	}
	

	public static void readGameInstanceFromStream(InputStream is, GameInstance gi) throws JDOMException, IOException
	{
		Document doc = new SAXBuilder().build(is);
    	Element root = doc.getRootElement();
    	
    	for (Element elem : root.getChildren())
    	{
    		String name = elem.getName();
    		//System.out.println("name" + name);
    		if (name.equals("name"))
    		{
	    		gi.name = elem.getValue();	
    		}
    		else if (name.equals("object"))
    		{
    			String uniqueName = elem.getAttributeValue("unique_name");
    			ObjectInstance oi = new ObjectInstance(gi.game.getObject(uniqueName), Integer.parseInt(elem.getAttributeValue("id")));
    			importState(oi.state, elem);
    			gi.objects.add(oi);
    		}
	   	}
	}

	public static void writeObjectInstanceToZip(Game game, ByteArrayOutputStream byteStream) {
		// TODO Auto-generated method stub
		
	}

	public static void writeObjectToZip(Game game, ByteArrayOutputStream byteStream) {
		// TODO Auto-generated method stub
		
	}

	public static void writePlayerToZip(Player player, ByteArrayOutputStream byteStream) {
		// TODO Auto-generated method stub
		
	}
	
}
