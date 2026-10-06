package com.mineblox.core;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.*;

public final class MineBloXPlugin extends JavaPlugin implements Listener {
    private final Map<String, Place> places = new LinkedHashMap<>();
    private final Map<UUID, Long> cooldown = new HashMap<>();
    private final Set<UUID> editors = new HashSet<>();

    @Override public void onEnable() { saveDefaultConfig(); loadPlaces(); Bukkit.getPluginManager().registerEvents(this,this); getLogger().info("MineBloX enabled: " + places.size() + " games"); }
    @Override public void onDisable() { savePlaces(); }
    private void loadPlaces(){ places.clear(); ConfigurationSection s=getConfig().getConfigurationSection("places"); if(s==null)return; for(String id:s.getKeys(false)) places.put(id.toLowerCase(Locale.ROOT),new Place(id,s.getString(id+".display-name",id),s.getString(id+".description",""),s.getString(id+".world",id))); }
    private void savePlaces(){ getConfig().set("places",null); for(Place p:places.values()){String k="places."+p.id;getConfig().set(k+".display-name",p.displayName);getConfig().set(k+".description",p.description);getConfig().set(k+".world",p.worldName);} saveConfig(); }
    private String c(String s){return ChatColor.translateAlternateColorCodes('&',s);}
    private ItemStack item(Material m,String name,String... lore){ItemStack x=new ItemStack(m);ItemMeta im=x.getItemMeta();im.setDisplayName(c(name));im.setLore(Arrays.stream(lore).map(this::c).toList());x.setItemMeta(im);return x;}

    private void main(Player p){
        Inventory i=Bukkit.createInventory(null,27,c("§b§lMineBloX"));
        i.setItem(10,item(Material.COMPASS,"§a§lИГРАТЬ","§7Открыть выбор игр"));
        i.setItem(12,item(Material.NETHER_STAR,"§e§lПОПУЛЯРНЫЕ","§7Популярные игры MineBloX"));
        i.setItem(14,item(Material.PLAYER_HEAD,"§b§lПРОФИЛЬ","§7Профиль игрока"));
        i.setItem(16,item(Material.BRICKS,"§d§lСТУДИЯ","§7Редактор плейсов (для OP)"));
        i.setItem(22,item(Material.BOOK,"§f§lINFO","§7MineBloX без обязательных модов"));
        p.openInventory(i);
    }
    private void games(Player p){
        int size=Math.max(27,Math.min(54,((places.size()+8)/9)*9));
        Inventory i=Bukkit.createInventory(null,size,c("§9§lMineBloX §f§lGames"));
        int n=0; for(Place q:places.values()){ if(n>=size-1)break; i.setItem(n++,item(Material.GRASS_BLOCK,q.displayName,"§7"+q.description,"§7Игроков: §f"+online(q.worldName),"§aНажми, чтобы играть")); }
        i.setItem(size-1,item(Material.BARRIER,"§c§lНАЗАД","§7В главное меню")); p.openInventory(i);
    }
    private void studioMenu(Player p){
        if(!p.hasPermission("mineblox.admin")){p.sendMessage(c("§cСтудия доступна администраторам."));return;}
        Inventory i=Bukkit.createInventory(null,27,c("§d§lMineBloX Studio"));
        i.setItem(10,item(Material.CRAFTING_TABLE,"§a§lРЕДАКТИРОВАТЬ","§7Выбрать плейс и открыть его в редакторе"));
        i.setItem(12,item(Material.GRASS_BLOCK,"§2§lСОЗДАТЬ ПЛЕЙС","§7Создание плейса выполняется командой","§f/place create <id> <world> <название>"));
        i.setItem(14,item(Material.REDSTONE_BLOCK,"§c§lВЫЙТИ ИЗ РЕДАКТОРА","§7Вернуть обычный режим"));
        i.setItem(16,item(Material.BOOK,"§f§lКОМАНДЫ","§7/studio <id>","§7/place list"));
        i.setItem(22,item(Material.BARRIER,"§c§lНАЗАД","§7В главное меню")); p.openInventory(i);
    }
    private void editorGames(Player p){
        Inventory i=Bukkit.createInventory(null,27,c("§d§lStudio §f§lSelect Game")); int n=0;
        for(Place q:places.values()){if(n>=26)break;i.setItem(n++,item(Material.BRICKS,q.displayName,"§7Мир: §f"+q.worldName,"§aНажми, чтобы открыть редактор"));}
        i.setItem(26,item(Material.BARRIER,"§c§lНАЗАД","§7В Studio"));p.openInventory(i);
    }
    private int online(String w){World x=Bukkit.getWorld(w);return x==null?0:x.getPlayers().size();}

    private void enterEditor(Player p,Place q){
        World w=Bukkit.getWorld(q.worldName);
        if(w==null){p.sendMessage(c("§cМир §f"+q.worldName+" §cне загружен. Сначала импортируй/создай его на хостинге."));return;}
        editors.add(p.getUniqueId());p.teleport(w.getSpawnLocation());p.setGameMode(GameMode.CREATIVE);p.setFlying(true);p.sendMessage(c("§d§lMineBloX Studio §7» §f"+q.displayName));
        p.sendMessage(c("§7Ты в редакторе. Строй обычными инструментами Minecraft."));
        p.sendMessage(c("§7Команды: §f/studio save " + q.id + " §7| §f/studio exit"));
        p.sendMessage(c("§eПодсказка: §fиспользуй Creative-инвентарь для моделирования сцены, зданий и объектов."));
    }
    private void exitEditor(Player p){ editors.remove(p.getUniqueId());p.setFlying(false);p.setGameMode(GameMode.ADVENTURE);p.sendMessage(c("§dMineBloX Studio §7» §fРедактор закрыт.")); }

    @EventHandler public void quit(PlayerQuitEvent e){editors.remove(e.getPlayer().getUniqueId());}
    @EventHandler public void click(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p))return;String t=ChatColor.stripColor(e.getView().getTitle());if(!t.startsWith("MineBloX")&&!t.startsWith("Studio"))return;e.setCancelled(true);if(e.getCurrentItem()==null)return;
        if(t.equals("MineBloX")){switch(e.getRawSlot()){case 10,12 -> games(p);case 16 -> studioMenu(p);default -> {}}return;}
        if(t.equals("MineBloX Games")){if(e.getCurrentItem().getType()==Material.BARRIER){main(p);return;}int s=e.getRawSlot();List<Place> l=new ArrayList<>(places.values());if(s>=0&&s<l.size())teleport(p,l.get(s));return;}
        if(t.equals("MineBloX Studio")){if(e.getCurrentItem().getType()==Material.BARRIER){main(p);return;}if(e.getRawSlot()==10)editorGames(p);return;}
        if(t.equals("Studio Select Game")){if(e.getCurrentItem().getType()==Material.BARRIER){studioMenu(p);return;}int s=e.getRawSlot();List<Place> l=new ArrayList<>(places.values());if(s>=0&&s<l.size())enterEditor(p,l.get(s));}
    }
    private void teleport(Player p,Place q){long now=System.currentTimeMillis();if(now-cooldown.getOrDefault(p.getUniqueId(),0L)<1000)return;cooldown.put(p.getUniqueId(),now);World w=Bukkit.getWorld(q.worldName);if(w==null){p.sendMessage(c("§cMineBloX: мир §f"+q.worldName+" §cне загружен."));return;}p.teleport(w.getSpawnLocation());p.sendMessage(c("§bMineBloX §7» §fВход в "+q.displayName));}

    @Override public boolean onCommand(CommandSender s,Command cmd,String label,String[] a){
        String n=cmd.getName().toLowerCase(Locale.ROOT);
        if(n.equals("mineblox")||n.equals("places")){if(!(s instanceof Player p)){s.sendMessage("Players only.");return true;}if(n.equals("mineblox"))main(p);else games(p);return true;}
        if(n.equals("studio")){if(!(s instanceof Player p)){s.sendMessage("Players only.");return true;}if(!p.hasPermission("mineblox.admin")){p.sendMessage(c("§cNo permission."));return true;}if(a.length==0){studioMenu(p);return true;}if(a[0].equalsIgnoreCase("exit")){exitEditor(p);return true;}if(a[0].equalsIgnoreCase("save")&&a.length>=2){p.sendMessage(c("§aСохранение мира выполняется самим Minecraft/хостингом. Плейс §f"+a[1]+" §aготов к публикации."));return true;}Place q=places.get(a[0].toLowerCase(Locale.ROOT));if(q==null){p.sendMessage(c("§cПлейс не найден. /place list"));return true;}enterEditor(p,q);return true;}
        if(!n.equals("place"))return false;if(!s.hasPermission("mineblox.admin")){s.sendMessage(c("§cNo permission."));return true;}
        if(a.length==0||a[0].equalsIgnoreCase("list")){s.sendMessage(c("§b§lMineBloX games:"));for(Place q:places.values())s.sendMessage(c(" §f"+q.id+" §7→ "+q.displayName+" §8["+q.worldName+"]"));return true;}
        if(a[0].equalsIgnoreCase("create")&&a.length>=3){String id=a[1].toLowerCase(Locale.ROOT),w=a[2];if(places.containsKey(id)){s.sendMessage(c("§cЭтот ID уже существует."));return true;}String name=a.length>=4?String.join(" ",Arrays.copyOfRange(a,3,a.length)):id;places.put(id,new Place(id,"§a"+name,"§7MineBloX game",w));savePlaces();s.sendMessage(c("§aСоздан плейс §f"+id+"§a. Открой /studio "+id));return true;}
        if(a[0].equalsIgnoreCase("delete")&&a.length>=2){places.remove(a[1].toLowerCase(Locale.ROOT));savePlaces();s.sendMessage(c("§aПлейс удалён (если существовал)."));return true;}
        if(a[0].equalsIgnoreCase("setspawn")&&a.length>=2&&s instanceof Player p){Place q=places.get(a[1].toLowerCase(Locale.ROOT));if(q==null){p.sendMessage(c("§cПлейс не найден."));return true;}q.worldName=p.getWorld().getName();p.getWorld().setSpawnLocation(p.getLocation());savePlaces();p.sendMessage(c("§aМир плейса сохранён: §f"+q.id));return true;}
        s.sendMessage(c("§e/place list"));s.sendMessage(c("§e/place create <id> <world> <название>"));s.sendMessage(c("§e/place delete <id>"));s.sendMessage(c("§e/place setspawn <id>"));return true;
    }
    private static final class Place {final String id,displayName,description;String worldName;Place(String i,String n,String d,String w){id=i;displayName=n;description=d;worldName=w;}}
}
