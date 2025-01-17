package net.multiplemonomials.mobdeathmessages.chat;

import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.HashMap;

import org.atteo.evo.inflector.English;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import net.minecraft.init.SoundEvents;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.translation.I18n;

import net.multiplemonomials.mobdeathmessages.MobDeathMessages;
import net.multiplemonomials.mobdeathmessages.configuration.ModConfiguration;
import net.multiplemonomials.mobdeathmessages.data.IMDMPlayerData;
import net.multiplemonomials.mobdeathmessages.reference.Names;
import net.multiplemonomials.mobdeathmessages.util.LogHelper;
import net.multiplemonomials.mobdeathmessages.util.NameUtils;

public class KillingSpreeMessager
{
	
	/**
	 * HashMap of entity class names and kill scores to keep track of how many kills types of mob have gotten
	 */
	public static HashMap<String, Integer> mobScores = new HashMap<String, Integer>();
	
	/**
	 * handle when a player kills something
	 * @param player
	 * @param EntityLiving the thing they killed
	 */
	public static void handlePlayerKill(EntityPlayer player, EntityLivingBase deadEntity)
	{
		IMDMPlayerData data = player.getCapability(MobDeathMessages.MDM_DATA_CAPABILITY, null);
				
		if(data.getKillScore() < 0)
		{
			//reset dying spree
			data.setKillScore(0);
		}		
		
		data.setKillScore(data.getKillScore() + 1);
		
		KillingSpree newSpree = KillingSpree.getKillingSpreeLevel(data.getKillScore());
		if(newSpree.ordinal() != data.getCurrentKillingSpree().ordinal())
		{
			//higher ordinals = better killing sprees
			if(newSpree.ordinal() > data.getCurrentKillingSpree().ordinal())
			{
				showKillingSpreeMessage(player.getDisplayName(), false, newSpree);
				
				//give the player XP
				int expAmount = ModConfiguration.xpForKillingSpree * (1 << (newSpree.ordinal() - KillingSpree.KILLINGSPREE.ordinal()));
				LogHelper.info("Giving " + player.getName() + " " + expAmount + " xp for their " + newSpree.toString().toLowerCase());
				player.addExperience(expAmount);
				player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, (float) 1, 1);
			}
			
			data.setCurrentKillingSpree(newSpree);
		}
	}
	
	/**
	 * handle killing sprees when a mob kills something
	 * @param attackingEntity
	 */
	public static void handleMobKill(EntityLiving attackingEntity)
	{
		String entityClass = attackingEntity.getClass().getSimpleName();
		
		int killScore = 0;
		
		if(mobScores.containsKey(entityClass))
		{
			killScore = mobScores.get(entityClass);
		}
		if(killScore < 0)
		{
			//reset dying spree
			killScore = 0;
		}
		++killScore;
		
		KillingSpree previousSpree = KillingSpree.getKillingSpreeLevel(killScore - 1);
		KillingSpree newSpree = KillingSpree.getKillingSpreeLevel(killScore);
		if(newSpree.ordinal() != previousSpree.ordinal())
		{
			//higher ordinals = better killing sprees
			if(newSpree.ordinal() > previousSpree.ordinal())
			{
	
				String friendlyPluralMobName = English.plural(NameUtils.getEntityNameForDisplay(attackingEntity));
				showKillingSpreeMessage(new TextComponentString(friendlyPluralMobName), true, newSpree);
			}
			
		}
		
		mobScores.put(entityClass, killScore);
	}
	
	/**
	 * handle killing sprees when a mob dies
	 * @param attackingEntity the mob that died
	 */
	public static void handleMobDeath(EntityLiving deadEntity)
	{
		String entityString = deadEntity.getClass().getSimpleName();
		
		int killScore = 0;
		
		if(mobScores.containsKey(entityString))
		{
			killScore = mobScores.get(entityString);
		}
		if(killScore > 0)
		{
			//reset killing spree
			killScore = 0;
		}
		--killScore;
		
		KillingSpree previousSpree = KillingSpree.getKillingSpreeLevel(killScore + 1);
		KillingSpree newSpree = KillingSpree.getKillingSpreeLevel(killScore);
		if(newSpree.ordinal() != previousSpree.ordinal())
		{
			//higher ordinals = worse killing sprees
			if(newSpree.ordinal() < previousSpree.ordinal())
			{
				String friendlyPluralMobName = English.plural(NameUtils.getEntityNameForDisplay(deadEntity));
				showKillingSpreeMessage(new TextComponentString(friendlyPluralMobName), true, newSpree);
			}
		}
		
		mobScores.put(entityString, killScore);
	}
	
	/**
	 * Handle player death, adjusting their score and dealing with Dying Sprees.
	 * @param player
	 */
	public static void handlePlayerDeath(EntityPlayer player)
	{
		IMDMPlayerData data = player.getCapability(MobDeathMessages.MDM_DATA_CAPABILITY, null);
		if(data.getKillScore() > 0)
		{
			//reset dying spree
			data.setKillScore(0);
		}
		else
		{
			data.setKillScore(data.getKillScore() - 1);
		}
		
		KillingSpree newSpree = KillingSpree.getKillingSpreeLevel(data.getKillScore());
		if(newSpree.ordinal() != data.getCurrentKillingSpree().ordinal())
		{
			//lower ordinals = better dying sprees
			if(newSpree.ordinal() < data.getCurrentKillingSpree().ordinal() && newSpree != KillingSpree.NONE)
			{
				showKillingSpreeMessage(player.getDisplayName(), false, newSpree);
			}
			data.setCurrentKillingSpree(newSpree);
		}
	}

	/**
	 * Broadcast the killing spree message to the server.
	 * @param entityName
	 * @param plural Whether or not the entityName is plural.
	 * @param newSpree
	 */
	// Only if I could update this to 1.13 (or newer)...
	private static void showKillingSpreeMessage(ITextComponent entityName, boolean plural, KillingSpree newSpree)
	{
		ITextComponent message = new TextComponentString(I18n.translateToLocal(Names.KillingSprees.MESSAGEPREFIX));
		message.appendSibling(entityName);
	
		//"escape" color codes in mob names (but keep advanced info)
		entityName.setStyle(entityName.getStyle().setColor(TextFormatting.WHITE));
		
		message.appendText(plural ? " are " : " is ");
		message.appendSibling(new TextComponentString(I18n.translateToLocal(newSpree.getText(plural))));
		
		FMLCommonHandler.instance().getMinecraftServerInstance().getPlayerList().sendMessage(message);
	}
}
