<#--
EmergentMUD - A modern MUD with a procedurally generated world.
Copyright (C) 2016-2017 Peter Keeler

This file is part of EmergentMUD.

EmergentMUD is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

EmergentMUD is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
-->
<#import "/spring.ftl" as spring>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta http-equiv="X-UA-Compatible" content="IE=edge"/>
    <title>EmergentMUD</title>
    <meta name="description" content=""/>
    <meta name="viewport" content="width=device-width"/>
    <base href="/"/>
    <link rel='stylesheet' type='text/css' href="<@spring.url 'https://fonts.googleapis.com/css?family=Inconsolata'/>">
    <link rel="stylesheet" type="text/css" href="<@spring.url '/webjars/bootstrap/css/bootstrap.min.css'/>"/>
    <link rel="stylesheet" type="text/css" href="<@spring.url '/webjars/font-awesome/css/font-awesome.min.css'/>"/>
    <link rel="stylesheet" type="text/css" href="<@spring.url '/css/bootstrap-social.css'/>"/>
    <link rel="stylesheet" type="text/css" href="<@spring.url '/css/index.css'/>">
    <link rel="stylesheet" type="text/css" href="<@spring.url '/css/commands.css'/>">
</head>
<body>

<div class="container-fluid">
    <div class="row">
        <div class="col-md-12 text-center heading">
            <span>EmergentMUD</span>
        </div>
    </div>
    <div class="row">
        <div class="col-md-10 col-md-offset-1 description">
            <div class="row">
                <div class="col-md-12 command-heading">
                    <p>The following are the emotes you can use while playing the game.</p>
                    <p>Emotes are simple messages you can send to other players to let them know what you're feeling
                    right now. They don't have any real effect on objects or players in the game, but they do a lot to
                    make interacting with other players more interesting and meaningful. The examples below assume that
                    your name is "${self.name}" and the target (if there is one) is named "${target.name}". In the
                    actual game the names will be replaced with your character's name and the name of whoever you are
                    targeting.</p>
                </div>
            </div>
            <#list metadataList as metadata>
                <div class="row">
                <div class="col-md-12 command">${metadata.name?upper_case}<br/>
                    <span style="color: deepskyblue;">If you type: ${metadata.name?upper_case}</span><br/>
                    <span style="color: deepskyblue;">You will see: ${emoteMap[metadata.name].toSelfUntargeted!}</span><br/>
                    <span style="color: deepskyblue;">Others will see: ${emoteMap[metadata.name].toRoomUntargeted!}</span><br/>
                    <span style="color: lawngreen;">If you type: ${metadata.name?upper_case} ${target.name?upper_case}</span><br/>
                    <span style="color: lawngreen;">You will see: ${emoteMap[metadata.name].toSelfWithTarget!}</span><br/>
                    <span style="color: lawngreen;">${target.name} will see: ${emoteMap[metadata.name].toTarget!}</span><br/>
                    <span style="color: lawngreen;">Others will see: ${emoteMap[metadata.name].toRoomWithTarget!}</span><br/>

                    <#if emoteMap[metadata.name].toSelfAsTarget?? && emoteMap[metadata.name].toRoomTargetingSelf??>
                        <span style="color: violet;">If you type: ${metadata.name?upper_case} SELF</span><br/>
                        <span style="color: violet;">You will see: ${emoteMap[metadata.name].toSelfAsTarget!}</span><br/>
                        <span style="color: violet;">Others will see: ${emoteMap[metadata.name].toRoomTargetingSelf!}</span><br/>
                    </#if>
                </div>
                </div>
            </#list>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12 text-center margin-bottom">
            [ <a href="<@spring.url '/'/>">Home</a> ]
            [ <a href="<@spring.url '/public/commands'/>">Commands</a> ]
        </div>
    </div>
    <div class="row">
        <div class="col-md-12 text-center margin-bottom">
            [ <a href="https://emergentmud.blogspot.com" target="_blank">Development Blog</a> ]
            [ <a href="https://bitbucket.org/scionaltera/emergentmud" target="_blank">Source Repository</a> ]
            [ <a href="https://tree.taiga.io/project/scionaltera-emergentmud/" target="_blank">Issue Tracker</a> ]
            [ <a href="https://hub.docker.com/r/scionaltera/emergentmud/" target="_blank">Docker Hub</a> ]
            [ <a href="http://uptime.emergentmud.com" target="_blank">Server Uptime</a> ]
        </div>
    </div>
</div>

<script type="text/javascript" src="/webjars/jquery/jquery.min.js"></script>
<script type="text/javascript" src="/webjars/bootstrap/js/bootstrap.min.js"></script>
<script type="text/javascript" src="//s7.addthis.com/js/300/addthis_widget.js#pubid=ra-57cd31b577134d6f"></script>
<script src="//static.getclicky.com/js" type="text/javascript"></script>
<script type="text/javascript">try{ clicky.init(100986651); }catch(e){}</script>
<noscript><p><img alt="Clicky" width="1" height="1" src="//in.getclicky.com/100986651ns.gif" /></p></noscript>
</body>
</html>