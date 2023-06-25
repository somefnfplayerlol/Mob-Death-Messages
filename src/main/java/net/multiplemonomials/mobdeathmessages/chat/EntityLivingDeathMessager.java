package net.multiplemonomials.mobdeathmessages.chat;

import net.multiplemonomials.mobdeathmessages.configuration.ModConfiguration;
//import net.multiplemonomials.mobdeathmessages.util.NameUtils; hmm

import net.minecraftforge.fml.common.FMLCommonHandler;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;

import net.minecraft.entity.monster.EntityBlaze;

import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityTameable;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;

import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public class EntityLivingDeathMessager 
{
	/**
	 * Return the currently running MinecraftServer
	 * @return 
	 */
	private static MinecraftServer getMinecraftServer()
	{
		return FMLCommonHandler.instance().getMinecraftServerInstance();
	}
	
	/**
	 * Return whether a message should be shown for the dead mob based on the circumstances of death
	 * and the configuration of the mod
	 * @param deadEntity
	 * @param damageSource
	 * @return
	 */
	private static boolean shouldShowMessage(EntityLiving deadEntity, DamageSource damageSource)
	{
		boolean retval = false;
		
		//if we're only supposed to show messages involving named mobs, that gets priority
		if(ModConfiguration.showNamedMobsOnly)
		{
			if(ModConfiguration.showMobOnMobDeathMessages)
			{
				//check attacker
				if(damageSource instanceof EntityDamageSource)
				{
					EntityDamageSource entitySource = (EntityDamageSource)damageSource;
					retval = entitySource.getTrueSource() instanceof EntityLiving && ((EntityLiving)entitySource.getTrueSource()).hasCustomName();
				}
			}
			
			//check attackee
			if(!retval)
			{
				retval = deadEntity.hasCustomName();
			}
		}
		else
		{
			
			if(damageSource instanceof EntityDamageSource)
			{
				EntityDamageSource entitySource = (EntityDamageSource)damageSource;
				if(entitySource.getTrueSource() instanceof EntityPlayer)
				{
					retval = ModConfiguration.showPlayerOnMobDeathMessages;
				}
				else
				{
					retval = ModConfiguration.showMobOnMobDeathMessages;
				}
			}
			else
			{
				if(!ModConfiguration.showBatsDyingOfNaturalCauses && deadEntity instanceof EntityBat)
				{
					retval = false;
				}
				else
				{
					retval = ModConfiguration.showInanimateObjectOnMobDeathMessages;
				}
			}
		}
		
		return retval;
	}
	
	/**
	 * Print the entity death message to chat
	 * @param deadEntity
	 * @param damageSource
	 */
	// Somehow TextComponentTranslation isn't a deprecated class in Forge 1.12.2 (well at least according to the Forge documentary)
	public static void showDeathMessage(EntityLiving deadEntity, DamageSource damageSource)
	{
		if(shouldShowMessage(deadEntity, damageSource))
		{
			ITextComponent deathMessage = deadEntity.getCombatTracker().getDeathMessage();
			
			//try to fix entities that aren't named properly in the death message
			//deathMessage = NameUtils.trimEntityNamesInString(deathMessage);
	
			//stop this silliness
			if(deadEntity instanceof EntitySquid)
			{
				if (damageSource == DamageSource.DROWN)
				{
					EntityLivingBase attacker = deadEntity.getCombatTracker().getBestAttacker();
					if (attacker != null)
					{
						deathMessage = new TextComponentTranslation("death.attack.asphyxiation.player", deadEntity.getDisplayName(), attacker.getDisplayName());
					}
					else
					{
						deathMessage = new TextComponentTranslation("death.attack.asphyxiation", deadEntity.getDisplayName());
					}
				}
			}
			if(deadEntity instanceof EntityBlaze)
			{
				if (damageSource == DamageSource.DROWN)
				{
					EntityLivingBase attacker = deadEntity.getCombatTracker().getBestAttacker();
					if (attacker != null)
					{
						deathMessage = new TextComponentTranslation("death.attack.deadlyWater.player", deadEntity.getDisplayName(), attacker.getDisplayName());
					}
					else
					{
						deathMessage = new TextComponentTranslation("death.attack.deadlyWater", deadEntity.getDisplayName());
					}
				}
			}
			
			//no duplicated pet death messages
			MinecraftServer server = getMinecraftServer();
			PlayerList playerList = server.getPlayerList();
			if(deadEntity instanceof EntityTameable)
			{
				for (EntityPlayerMP player : playerList.getPlayers())
				{
					if(!(((EntityTameable)deadEntity).isOwner(player)))
					{
						player.sendMessage(deathMessage);
					}
				}
			}
			else
			{
				playerList.sendMessage(deathMessage);
			}
		}
	}
}
