/*
 * Copyright � 2014 - 2015 | Alexander01998 | All rights reserved.
 * 
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package tk.wurst_client.event;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import tk.wurst_client.event.events.*;
import tk.wurst_client.event.listeners.*;
import tk.wurst_client.gui.error.GuiError;

public class EventManager
{
	private static Set<ChatInputListener> chatInputListeners = Collections
		.synchronizedSet(new HashSet<ChatInputListener>());
	private static Set<ChatOutputListener> chatOutputListeners = Collections
		.synchronizedSet(new HashSet<ChatOutputListener>());
	private static Set<DeathListener> deathListeners = Collections
		.synchronizedSet(new HashSet<DeathListener>());
	private static Set<GUIRenderListener> guiRenderListeners = Collections
		.synchronizedSet(new HashSet<GUIRenderListener>());
	private static Set<LeftClickListener> leftClickListeners = Collections
		.synchronizedSet(new HashSet<LeftClickListener>());
	private static Set<PacketInputListener> packetInputListeners = Collections
		.synchronizedSet(new HashSet<PacketInputListener>());
	private static Set<RenderListener> renderListeners = Collections
		.synchronizedSet(new HashSet<RenderListener>());
	private static Set<UpdateListener> updateListeners = Collections
		.synchronizedSet(new HashSet<UpdateListener>());
	
	private static Queue<Event> eventQueue =
		new ConcurrentLinkedQueue<Event>();
	private static Queue<Runnable> listenerQueue =
		new ConcurrentLinkedQueue<Runnable>();
	private static boolean locked;
	
	public synchronized static void fireEvent(Event event)
	{
		if(locked)
		{
			eventQueue.add(event);
			return;
		}
		locked = true;
		try
		{
			if(event instanceof UpdateEvent)
			{
				Iterator<UpdateListener> itr = updateListeners.iterator();
				while(itr.hasNext())
				{
					UpdateListener listener = itr.next();
					try
					{
						listener.onUpdate();
					}catch(Exception e)
					{
						handleException(e, listener, "updating", "");
					}
				}
			}else if(event instanceof RenderEvent)
			{
				Iterator<RenderListener> itr = renderListeners.iterator();
				while(itr.hasNext())
				{
					RenderListener listener = itr.next();
					try
					{
						listener.onRender();
					}catch(Exception e)
					{
						handleException(
							e,
							listener,
							"rendering",
							"GUI screen: "
								+ Minecraft.getMinecraft().currentScreen != null
								? Minecraft.getMinecraft().currentScreen
									.getClass()
									.getSimpleName() : "null");
					}
				}
			}else if(event instanceof GUIRenderEvent)
			{
				Iterator<GUIRenderListener> itr = guiRenderListeners.iterator();
				while(itr.hasNext())
				{
					GUIRenderListener listener = itr.next();
					try
					{
						listener.onRenderGUI();
					}catch(Exception e)
					{
						handleException(
							e,
							listener,
							"rendering GUI",
							"GUI screen: "
								+ Minecraft.getMinecraft().currentScreen != null
								? Minecraft.getMinecraft().currentScreen
									.getClass()
									.getSimpleName() : "null");
					}
				}
			}else if(event instanceof PacketInputEvent)
			{
				Iterator<PacketInputListener> itr =
					packetInputListeners.iterator();
				while(itr.hasNext())
				{
					PacketInputListener listener = itr.next();
					try
					{
						listener.onReceivedPacket((PacketInputEvent)event);
					}catch(Exception e)
					{
						handleException(
							e,
							listener,
							"receiving packet",
							"Packet: "
								+ (((PacketInputEvent)event).getPacket() != null
									? ((PacketInputEvent)event).getPacket()
										.getClass()
										.getSimpleName() : "null"));
					}
				}
			}else if(event instanceof LeftClickEvent)
			{
				Iterator<LeftClickListener> itr = leftClickListeners.iterator();
				while(itr.hasNext())
				{
					LeftClickListener listener = itr.next();
					try
					{
						listener.onLeftClick();
					}catch(Exception e)
					{
						handleException(e, listener, "left-clicking", "");
					}
				}
			}else if(event instanceof ChatInputEvent)
			{
				Iterator<ChatInputListener> itr = chatInputListeners.iterator();
				while(itr.hasNext())
				{
					ChatInputListener listener = itr.next();
					try
					{
						listener.onReceivedMessage((ChatInputEvent)event);
					}catch(Exception e)
					{
						handleException(e, listener, "receiving chat message",
							"Message: `"
								+ ((ChatInputEvent)event).getComponent()
									.getUnformattedText() + "`");
					}
				}
			}else if(event instanceof ChatOutputEvent)
			{
				Iterator<ChatOutputListener> itr =
					chatOutputListeners.iterator();
				while(itr.hasNext())
				{
					ChatOutputListener listener = itr.next();
					try
					{
						listener.onSentMessage((ChatOutputEvent)event);
					}catch(Exception e)
					{
						handleException(
							e,
							listener,
							"sending chat message",
							"Message: `"
								+ ((ChatOutputEvent)event).getMessage()
								+ "`");
					}
				}
			}else if(event instanceof DeathEvent)
			{
				Iterator<DeathListener> itr = deathListeners.iterator();
				while(itr.hasNext())
				{
					DeathListener listener = itr.next();
					try
					{
						listener.onDeath();
					}catch(Exception e)
					{
						handleException(e, listener, "dying", "");
					}
				}
			}
			for(Runnable task; (task = listenerQueue.poll()) != null;)
				task.run();
		}catch(Exception e)
		{
			handleException(e, event, "processing events", "Event type: "
				+ event.getClass().getSimpleName());
			eventQueue.clear();
		}finally
		{
			locked = false;
			for(Event ev; (ev = eventQueue.poll()) != null;)
				fireEvent(ev);
		}
	}
	
	public synchronized static void handleException(final Exception e,
		final Object cause, final String action, final String comment)
	{
		addUpdateListener(new UpdateListener()
		{
			@Override
			public void onUpdate()
			{
				Minecraft.getMinecraft().displayGuiScreen(
					new GuiError(e, cause, action, comment));
				EventManager.removeUpdateListener(this);
			}
		});
	}
	
	public synchronized static void addChatInputListener(
		final ChatInputListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				chatInputListeners.add(listener);
			}
		});
	}
	
	public synchronized static void removeChatInputListener(
		final ChatInputListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				chatInputListeners.remove(listener);
			}
		});
	}
	
	public synchronized static void addChatOutputListener(
		final ChatOutputListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				chatOutputListeners.add(listener);
			}
		});
	}
	
	public synchronized static void removeChatOutputListener(
		final ChatOutputListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				chatOutputListeners.remove(listener);
			}
		});
	}
	
	public synchronized static void addDeathListener(
		final DeathListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				deathListeners.add(listener);
			}
		});
	}
	
	public synchronized static void removeDeathListener(
		final DeathListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				deathListeners.remove(listener);
			}
		});
	}
	
	public synchronized static void addGUIRenderListener(
		final GUIRenderListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				guiRenderListeners.add(listener);
			}
		});
	}
	
	public synchronized static void removeGUIRenderListener(
		final GUIRenderListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				guiRenderListeners.remove(listener);
			}
		});
	}
	
	public synchronized static void addLeftClickListener(
		final LeftClickListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				leftClickListeners.add(listener);
			}
		});
	}
	
	public synchronized static void removeLeftClickListener(
		final LeftClickListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				leftClickListeners.remove(listener);
			}
		});
	}
	
	public synchronized static void addPacketInputListener(
		final PacketInputListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				packetInputListeners.add(listener);
			}
		});
	}
	
	public synchronized static void removePacketInputListener(
		final PacketInputListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				packetInputListeners.remove(listener);
			}
		});
	}
	
	public synchronized static void addRenderListener(
		final RenderListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				renderListeners.add(listener);
			}
		});
	}
	
	public synchronized static void removeRenderListener(
		final RenderListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				renderListeners.remove(listener);
			}
		});
	}
	
	public synchronized static void addUpdateListener(
		final UpdateListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				updateListeners.add(listener);
			}
		});
	}
	
	public synchronized static void removeUpdateListener(
		final UpdateListener listener)
	{
		listenerQueue.add(new Runnable()
		{
			@Override
			public void run()
			{
				updateListeners.remove(listener);
			}
		});
	}
}
