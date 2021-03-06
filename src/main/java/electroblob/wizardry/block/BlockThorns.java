package electroblob.wizardry.block;

import electroblob.wizardry.registry.Spells;
import electroblob.wizardry.registry.WizardryBlocks;
import electroblob.wizardry.spell.Spell;
import electroblob.wizardry.tileentity.TileEntityPlayerSaveTimed;
import electroblob.wizardry.util.AllyDesignationSystem;
import electroblob.wizardry.util.MagicDamage;
import net.minecraft.block.*;
import net.minecraft.block.BlockDoublePlant.EnumBlockHalf;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

@Mod.EventBusSubscriber
public class BlockThorns extends BlockBush implements ITileEntityProvider {

	public static final int GROWTH_STAGES = 8;

	public static final PropertyInteger AGE = PropertyInteger.create("age", 0, GROWTH_STAGES-1);
	public static final PropertyEnum<EnumBlockHalf> HALF = PropertyEnum.create("half", EnumBlockHalf.class);

	public BlockThorns(){
		this.setDefaultState(this.blockState.getBaseState().withProperty(HALF, EnumBlockHalf.LOWER).withProperty(AGE, 7));
		this.setHardness(4);
		this.setSoundType(SoundType.PLANT);
		this.setCreativeTab(null);
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos){
		return FULL_BLOCK_AABB;
	}

	@Override
	public IBlockState getStateFromMeta(int meta){
		return this.getDefaultState().withProperty(HALF, EnumBlockHalf.values()[meta / GROWTH_STAGES]).withProperty(AGE, meta % GROWTH_STAGES);
	}

	@Override
	public int getMetaFromState(IBlockState state){
		return state.getValue(HALF).ordinal() * GROWTH_STAGES + state.getValue(AGE);
	}

//	@Override
//	public void updateTick(World world, BlockPos pos, IBlockState state, Random rand){
//
//		super.updateTick(world, pos, state, rand);
//
//		// Update the state, including on the client, but don't do a block update since it's only visual
//		if(state.getValue(AGE) < GROWTH_STAGES-1) world.setBlockState(pos, state.withProperty(AGE, state.getValue(AGE) + 1), 2);
//	}

	@Override
	protected BlockStateContainer createBlockState(){
		return new BlockStateContainer(this, HALF, AGE);
	}

	public void placeAt(World world, BlockPos lowerPos, int flags){
		world.setBlockState(lowerPos, this.getDefaultState().withProperty(HALF, EnumBlockHalf.LOWER).withProperty(AGE, 0), flags);
		world.setBlockState(lowerPos.up(), this.getDefaultState().withProperty(HALF, EnumBlockHalf.UPPER).withProperty(AGE, 0), flags);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack){
		world.setBlockState(pos.up(), this.getDefaultState().withProperty(HALF, EnumBlockHalf.UPPER), 2);
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state){
		super.breakBlock(world, pos, state);
		if(state.getValue(HALF) == EnumBlockHalf.LOWER){
			if(world.getBlockState(pos.up()).getBlock() == this){
				world.destroyBlock(pos.up(), false);
			}
		}else{
			if(world.getBlockState(pos.down()).getBlock() == this){
				world.destroyBlock(pos.down(), false);
			}
		}
	}

	public boolean canBlockStay(World worldIn, BlockPos pos, IBlockState state){
		if(state.getValue(HALF) == BlockDoublePlant.EnumBlockHalf.UPPER){
			return worldIn.getBlockState(pos.down()).getBlock() == this;
		}else{
			IBlockState iblockstate = worldIn.getBlockState(pos.up());
			return iblockstate.getBlock() == this && this.canSustainBush(worldIn.getBlockState(pos.down()));
		}
	}

//	@Override
//	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos){
//		// Copied from BlockFlowerPot on authority of the Forge docs, which says this check is necessary
//		SoundLoopSpellDispenser tileentity = world instanceof ChunkCache ? ((ChunkCache)world).getTileEntity(pos, Chunk.EnumCreateEntityType.CHECK) : world.getTileEntity(pos);
//
//		if(tileentity instanceof TileEntityPlayerSaveTimed){
//			state = state.withProperty(AGE, Math.min(7, ((TileEntityPlayerSaveTimed)tileentity).timer/2));
//		}else{
//			state = state.withProperty(AGE, 7);
//		}
//
//		return state;
//	}

	@Override
	public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity){
		if(!world.isRemote){
			if(applyThornDamage(world, pos, entity)){
				entity.setInWeb();
			}
		}
	}

	private static boolean applyThornDamage(World world, BlockPos pos, Entity target){

		DamageSource source = DamageSource.CACTUS;

		TileEntity tileentity = world.getTileEntity(pos);

		if(tileentity instanceof TileEntityPlayerSaveTimed){

			EntityLivingBase caster = ((TileEntityPlayerSaveTimed)tileentity).getCaster();

			if(caster != null && AllyDesignationSystem.isValidTarget(caster, target)){
				source = MagicDamage.causeDirectMagicDamage(caster, MagicDamage.DamageType.MAGIC);
			}else{
				return false; // Don't attack or slow allies of the caster
			}
		}

		if(world.getTotalWorldTime() % 20 == 0) target.attackEntityFrom(source, Spells.forest_of_thorns.getProperty(Spell.DAMAGE).floatValue());

		return true;
	}

	@Override
	public Block.EnumOffsetType getOffsetType(){
		return Block.EnumOffsetType.XZ;
	}

	@Override
	public TileEntity createNewTileEntity(World world, int metadata){
		return new TileEntityPlayerSaveTimed(600);
	}

	@Override
	public boolean hasTileEntity(IBlockState state){
		return true;
	}

	@Override public boolean isReplaceable(IBlockAccess world, BlockPos pos){ return false; }
	@Override protected boolean canSustainBush(IBlockState state){ return state.isNormalCube(); }
	@Override public Item getItemDropped(IBlockState state, Random rand, int fortune){ return Items.AIR; }
	@Override public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player){ return false; }

	@SubscribeEvent
	public static void onLeftClickBlockEvent(PlayerInteractEvent.LeftClickBlock event){
		if(!event.getWorld().isRemote && event.getWorld().getTotalWorldTime() % 20 == 0
				&& event.getWorld().getBlockState(event.getPos()).getBlock() == WizardryBlocks.thorns){
			applyThornDamage(event.getWorld(), event.getPos(), event.getEntity());
		}
	}

}
