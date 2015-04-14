package com.fredtargaryen.floocraft.block;

import com.fredtargaryen.floocraft.DataReference;
import com.fredtargaryen.floocraft.FloocraftBase;
import com.fredtargaryen.floocraft.client.gui.GuiTeleport;
import com.fredtargaryen.floocraft.proxy.ClientProxy;
import net.minecraft.block.Block;
import net.minecraft.block.BlockFire;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GreenFlames extends Block
{
    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 9);
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public GreenFlames()
    {
        super(Material.air);
        this.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 2.0F, 1.0F);
    }

    @Override
    public void updateTick(World w, BlockPos pos, IBlockState state, Random par5Random)
    {
        if(state.getValue(AGE).equals(0) || !isInFireplace(w, pos))
        {
            w.setBlockState(pos, Blocks.fire.getDefaultState());
        }
        if(w.getClosestPlayer((double)pos.getX() + 0.5D, (double)pos.getY() + 0.5D, (double)pos.getZ() + 0.5D, (double) DataReference.FLOO_FIRE_DETECTION_RANGE) == null)
        {
            w.setBlockState(pos, state.withProperty(ACTIVE, false), 2);
        }
        else
        {
            w.setBlockState(pos, state.withProperty(ACTIVE, true), 2);
        }
        w.scheduleUpdate(pos, this, this.tickRate(w) + par5Random.nextInt(10));
    }

    @Override
    public void onEntityCollidedWithBlock(World w, BlockPos pos, IBlockState state, Entity e)
    {
        if(e instanceof EntityPlayer)
        {
            if(w.isRemote)
            {
                doClientGuiTings(pos.getX(), pos.getY(), pos.getZ());
            }
        }
    }

    @SideOnly(Side.CLIENT)
    public void doClientGuiTings(int x, int y, int z)
    {
        ClientProxy proxy = (ClientProxy) FloocraftBase.proxy;
        if(Minecraft.getMinecraft().currentScreen == null && !proxy.overrideTicker.isOverriding())
        {
            Minecraft.getMinecraft().displayGuiScreen(new GuiTeleport(x, y, z));
            proxy.overrideTicker.start();
        }
    }

    @Override
    public int tickRate(World par1World)
    {
        return 30;
    }

    @Override
    public void onBlockAdded(World w, BlockPos pos, IBlockState state)
    {
        if (!isInFireplace(w, pos))
        {
            w.setBlockState(pos, Blocks.fire.getDefaultState());
        }
        else
        {
            w.scheduleUpdate(pos, this, this.tickRate(w));
        }
    }

    /**
     * ALL FIREPLACE VALIDATION CODE STARTS HERE
     */
    protected int getTopBlockY(World w, BlockPos oldpos)
    {
        BlockPos pos = oldpos;
        pos.up();
        Block b0 = w.getBlockState(pos).getBlock();
        if (b0 == Blocks.air)
        {
            pos.up();
            while (w.getBlockState(pos).getBlock() == Blocks.air)
            {
                pos.up();
            }
            if (w.getBlockState(pos).getBlock().isNormalCube(w, pos))
            {
                return pos.getY();
            }
        }
        return 0;
    }

    protected boolean isWallColumn(World w, BlockPos oldbottom, int topY)
    {
        BlockPos bottom = oldbottom;
        if (topY == 0)
        {
            return false;
        }
        else
        {
            boolean valid = true;
            while (valid && bottom.getY() < topY) {
                if(w.getBlockState(bottom).getBlock().isNormalCube(w, bottom))
                {
                    bottom.up();
                }
                else {
                    valid = false;
                }
            }
            return valid;
        }
    }

    protected List<Integer> getWalls(World w, BlockPos oldbottom, int topY)
    {
        BlockPos bottom = oldbottom;
        List<Integer> walls = new ArrayList<Integer>();
        //z + 1
        bottom.add(0.0D, 0.0D, 1.0D);
        if(this.isWallColumn(w, bottom, topY))
        {
            walls.add(2);
        }
        //x - 1
        bottom.add(-1.0D, 0.0D, -1.0D);
        if(this.isWallColumn(w, bottom, topY))
        {
            walls.add(4);
        }
        //x + 1
        bottom.add(2.0D, 0.0D, 0.0D);
        if(this.isWallColumn(w, bottom, topY))
        {
            walls.add(6);
        }
        //z - 1
        bottom.add(0.0D, 0.0D, -1.0D);
        if(this.isWallColumn(w, bottom, topY))
        {
            walls.add(8);
        }
        return walls;
    }

    protected boolean canLoopToCorner(World w, BlockPos pos, int backWall, int oldSideWall, int top)
    {
        int sideWall;
        int oldX = pos.getX();
        int x = oldX;
        int oldZ = pos.getZ();
        int z = oldZ;
        int y = pos.getY();
        switch(oldSideWall)
        {
            case 2:
                sideWall = 8;
                break;
            case 4:
                sideWall = 6;
                break;
            case 6:
                sideWall = 4;
                break;
            case 8:
                sideWall = 2;
                break;
            default:
                return false;
        }
        boolean stop = false;
        while(!stop)
        {
            if(backWall == 2 || backWall == 8)
            {
                if(sideWall == 4){x--;}
                else{x++;}
            }
            else
            {
                if(sideWall == 2){z++;}
                else{z--;}
            }
            int newTop = this.getTopBlockY(w, pos);
            List<Integer> walls = this.getWalls(w, new BlockPos(x, y, z), newTop);
            switch(walls.size())
            {
                case 1:
                    if(!walls.contains(backWall))
                    {
                        return false;
                    }
                    else
                    {
                        if(newTop > top++)
                        {
                            if(!this.isWallColumn(w, new BlockPos(oldX, top, oldZ), newTop))
                            {
                                return false;
                            }
                        }
                        else if(newTop < top--)
                        {
                            if(!this.isWallColumn(w, new BlockPos(x, newTop, z), top))
                            {
                                return false;
                            }
                        }
                        oldX = x;
                        top = newTop;
                        oldZ = z;
                    }
                    break;
                case 2:
                    if(walls.contains(backWall) && walls.contains(sideWall))
                    {
                        stop = true;
                    }else{return false;}
                    break;
                default:return false;
            }
        }
        return true;
    }

    protected boolean isInFireplace(World w, BlockPos pos)
    {
        if(!w.canBlockSeeSky(pos))
        {
            if(pos.getY() < 254 && pos.getX() < 30000000 && pos.getX() > -30000000 && pos.getZ() < 30000000 && pos.getZ() > -30000000)
            {
                int t = this.getTopBlockY(w, pos);
                List<Integer> walls = this.getWalls(w, pos, t);
                switch(walls.size())
                {
                    case 3:
                        return true;
                    case 2:
                        if((walls.contains(2) && (walls.contains(4) || walls.contains(6))
                                || (walls.contains(8) && (walls.contains(4) || walls.contains(6)))))
                        {
                            return this.canLoopToCorner(w, pos, walls.get(0), walls.get(1), t)
                                    || this.canLoopToCorner(w, pos, walls.get(1), walls.get(0), t);
                        }
                        break;
                    case 1:
                        switch(walls.get(0))
                        {
                            case 2:
                                return this.canLoopToCorner(w, pos, 2, 4, t) && this.canLoopToCorner(w, pos, 2, 6, t);
                            case 4:
                                return this.canLoopToCorner(w, pos, 4, 8, t) && this.canLoopToCorner(w, pos, 4, 2, t);
                            case 6:
                                return this.canLoopToCorner(w, pos, 6, 8, t) && this.canLoopToCorner(w, pos, 6, 2, t);
                            case 8:
                                return this.canLoopToCorner(w, pos, 8, 4, t) && this.canLoopToCorner(w, pos, 8, 6, t);
                            default:break;
                        }
                        break;
                    default:break;
                }
            }
        }
        return false;
    }
}