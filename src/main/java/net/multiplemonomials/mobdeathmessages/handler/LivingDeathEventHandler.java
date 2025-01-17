package net.multiplemonomials.mobdeathmessages.handler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.EntityDamageSource;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.multiplemonomials.mobdeathmessages.chat.EntityLivingDeathMessager;
import net.multiplemonomials.mobdeathmessages.chat.KillingSpreeMessager;
import net.multiplemonomials.mobdeathmessages.configuration.ModConfiguration;

public class LivingDeathEventHandler
{
	
	@SubscribeEvent
	public void onLivingDeathEvent(LivingDeathEvent event)
	{
		EntityLivingBase entity = event.getEntityLiving();
		if (entity instanceof EntityArmorStand)
		{
			return; // do not.
		}
		
		if(entity.isServerWorld())
		{
			if(entity instanceof EntityPlayer)
			{
				if(entity instanceof FakePlayer)
				{
					return;
				}
				//handle player death
				if(ModConfiguration.killingSpreePlayersEnabled)
				{
					KillingSpreeMessager.handlePlayerDeath((EntityPlayer) entity);
				}
				
				//handle mob kill
				if(ModConfiguration.killingSpreePlayersVsMobsEnabled && event.getSource() instanceof EntityDamageSource)
				{
					EntityDamageSource entityDamageSource = (EntityDamageSource)event.getSource();
					Entity sourceEntity = entityDamageSource.getTrueSource();
					
					
					// Despite what the trueSource() documentation says, 
					// sometimes the source entity is not an EntityLiving and we have to do extra work.
					if(sourceEntity instanceof EntityArrow)
					{
						EntityArrow arrow = (EntityArrow)sourceEntity;
						sourceEntity = arrow.shootingEntity;
					}
					
					
					if(sourceEntity instanceof EntityPlayer)
					{
						KillingSpreeMessager.handlePlayerKill((EntityPlayer)sourceEntity, entity);
					}
					else
					{
						KillingSpreeMessager.handleMobKill((EntityLiving) sourceEntity);
					}
					
				}
					
			}
			else
			{
				if(event.getSource() instanceof EntityDamageSource)
				{
					EntityDamageSource entitySource = (EntityDamageSource)event.getSource();
					
					Entity source = entitySource.getTrueSource();
					
					if(source != null)
					{
						if(source instanceof EntityPlayer)
						{
							EntityPlayer attackingPlayer = (EntityPlayer)source;
							if(ModConfiguration.killingSpreePlayersVsMobsEnabled)
							{
								KillingSpreeMessager.handleMobDeath((EntityLiving) entity);
							}
							if(ModConfiguration.killingSpreePlayersEnabled)
							{
								KillingSpreeMessager.handlePlayerKill(attackingPlayer, (EntityLiving) entity);
							}
						}
						else if(ModConfiguration.killingSpreeMobsVsMobsEnabled)
						{
							KillingSpreeMessager.handleMobKill((EntityLiving) source);
							KillingSpreeMessager.handleMobDeath((EntityLiving) entity);
						}
					}
				}
				EntityLivingDeathMessager.showDeathMessage(((EntityLiving)entity), event.getSource());
				
			}
		}
		
	}
}
