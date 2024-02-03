/*
 * Copyright (c) 2020, PresNL
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.hidewidgets;
import com.google.inject.Provides;
import java.awt.Component;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.ScriptID;
import net.runelite.api.events.CanvasSizeChanged;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;

@PluginDescriptor(
        name = "Hide Widgets",
        description = "Hides all widgets (Resizable only)",
        tags = {}
)
@Slf4j
public class HideWidgetsPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private HideWidgetsConfig config;

    @Inject
    private KeyManager keyManager;

    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private HideWidgetsKeyboardListener hideWidgetsKeyboardListener;

    private Boolean hide = false;

    @Provides
    private HideWidgetsConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(HideWidgetsConfig.class);
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired scriptPostFired)
    {
        // 903 seems to get called when something opens the inventory like when banking or when opening stores
        if (scriptPostFired.getScriptId() == ScriptID.TOPLEVEL_REDRAW || scriptPostFired.getScriptId() == 903)
        {
            if (hide)
			{
				hideWidgets(true);
			}
        }
    }

    @Subscribe
    public void onCanvasSizeChanged(CanvasSizeChanged canvasSizeChanged)
    {
        // hiding in fixed mode does not actually hide stuff and might break stuff so let's not do that
        if (!client.isResized())
		{
			hideWidgets(false);
		}
    }

    @Override
    protected void startUp() throws Exception
    {
        keyManager.registerKeyListener(hideWidgetsKeyboardListener);
        hide = false;
        hideWidgets(false);
    }

    @Override
    protected void shutDown() throws Exception
    {
        keyManager.unregisterKeyListener(hideWidgetsKeyboardListener);
        hideWidgets(false);
    }

    public void toggle()
    {
        log.debug("toggled hiding widgets");
        hide = !hide;
        hideWidgets(hide);
    }

    protected void hideWidgetChildren(Widget root, boolean hide)
    {
        // The normal GetChildren function seems to always return 0 so we get all the different types
        // of other children instead and merge them into one array
        Widget[] rootDynamicChildren = root.getDynamicChildren();
        Widget[] rootNestedChildren = root.getNestedChildren();
        Widget[] rootStaticChildren = root.getStaticChildren();

        Widget[] rootChildren = new Widget[rootDynamicChildren.length + rootNestedChildren.length + rootStaticChildren.length];
        System.arraycopy(rootDynamicChildren, 0, rootChildren,0 , rootDynamicChildren.length);
        System.arraycopy(rootNestedChildren, 0, rootChildren,rootDynamicChildren.length , rootNestedChildren.length);
        System.arraycopy(rootStaticChildren, 0, rootChildren,rootDynamicChildren.length + rootNestedChildren.length, rootStaticChildren.length);

        if (rootChildren != null) {
            for (Widget w : rootChildren) {
                if (w != null) {
                    // hiding the widget with content type 1337 prevents the game from rendering so let's not do that
                    if (w.getContentType() != 1337)
                        w.setHidden(hide);
                }
            }
        }
    }

    protected void hideWidgets(boolean hide)
    {
        // hiding in fixed mode does not actually hide stuff and might break stuff so let's not do that
        if (hide && !client.isResized())
        {
            hideWidgets(false);
        }
        else
        {
            clientThread.invokeLater(() ->
            {
                // modern resizeable
                Widget modernResizableMinimap = client.getWidget(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP);
                if (modernResizableMinimap != null)
				{
					Widget modernResizableParent = modernResizableMinimap.getParent();
					if (modernResizableParent != null)
					{
						hideWidgetChildren(modernResizableParent, hide);
					}
				}

                // classic resizeable
                Widget classicResizableMinimap = client.getWidget(ComponentID.RESIZABLE_VIEWPORT_MINIMAP);
                if (classicResizableMinimap != null)
				{
					Widget classicResizableParent = classicResizableMinimap.getParent();
					if (classicResizableParent != null)
					{
						hideWidgetChildren(classicResizableParent, hide);
					}
				}
            });
        }

    }
}
