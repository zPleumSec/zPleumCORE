// app/dashboard/DashboardClient.tsx
"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Image from "next/image";

interface TeamMember {
  discord_id: number;
  discordName: string;
  discordAvatar: string;
  playerName: string;
  ip_address: string;
  online: boolean;
  preJoin: boolean;
  verified_timestamp: string;
}

interface DashboardClientProps {
  playerName: string;
}

export default function DashboardClient({ playerName }: DashboardClientProps) {
  const [teamMembers, setTeamMembers] = useState<TeamMember[]>([]);
  const [currentUser, setCurrentUser] = useState<TeamMember | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [logoutLoading, setLogoutLoading] = useState(false);
  const router = useRouter();


  const fetchTeamData = async () => {
    try {
      const res = await fetch("/api/dashboard/team-members", {
        credentials: "include",
      });

      const data = await res.json();

      if (!res.ok) {
        setError(data.error || "Failed to fetch team data");
        setLoading(false);
        return;
      }

      setTeamMembers(data.teamMembers || []);
      setCurrentUser(data.currentUser || null);
      setLoading(false);
    } catch {
      setError("Error fetching team data");
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTeamData();
    const interval = setInterval(fetchTeamData, 30000);
    return () => clearInterval(interval);
  }, []);

  const handleLogout = async () => {
    setLogoutLoading(true);
    try {
      const res = await fetch("/api/auth/logout", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ playerName }),
      });

      if (res.ok) {
        // Clear cookie
        document.cookie = "playerName=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
        router.push("/");
      } else {
        const data = await res.json();
        setError(data.error || "Logout failed");
      }
    } catch (e) {
      console.error("Error logging out:", e);
      setError("Error logging out");
    } finally {
      setLogoutLoading(false);
    }
  };

  const formatVerifiedTime = (timestamp: string) => {
    if (!timestamp) return "ไม่ได้ยืนยัน";
    
    const verifiedDate = new Date(timestamp);
    const now = new Date();
    const diffMs = now.getTime() - verifiedDate.getTime();
    
    const diffMinutes = Math.floor(diffMs / (1000 * 60));
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));
    
    let timeAgo = "";
    if (diffDays > 0) {
      timeAgo = `${diffDays} วันที่แล้ว`;
    } else if (diffHours > 0) {
      timeAgo = `${diffHours} ชั่วโมงที่แล้ว`;
    } else if (diffMinutes > 0) {
      timeAgo = `${diffMinutes} นาทีที่แล้ว`;
    } else {
      timeAgo = "เพิ่งยืนยัน";
    }
    
    return {
      formatted: verifiedDate.toLocaleString('th-TH', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      }),
      ago: timeAgo
    };
  };

  const handleForceLogout = async (id: number) => {
    console.log("Force logout called for id:", id);
    try {
      const res = await fetch("/api/dashboard/force-logout", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ id }),
      });
  
      if (!res.ok) {
        const data = await res.json();
        console.error("Force logout error response:", data);
        setError(data.error || "บังคับออกจากระบบไม่สำเร็จ");
        return;
      }
  
      console.log("Force logout success");
      fetchTeamData();
    } catch (e) {
      console.error("Force logout failed:", e);
      setError("บังคับออกจากระบบไม่สำเร็จ");
    }
  };  

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
        <div className="flex items-center justify-center min-h-screen">
          <div className="bg-white/10 backdrop-blur-lg p-8 rounded-3xl shadow-2xl border border-white/20">
            <div className="flex flex-col items-center space-y-4">
              <div className="w-12 h-12 border-4 border-white/30 border-t-white rounded-full animate-spin"></div>
              <p className="text-white text-lg">กำลังโหลดข้อมูล...</p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-red-900 via-red-800 to-red-900">
        <div className="flex items-center justify-center min-h-screen">
          <div className="bg-white/10 backdrop-blur-lg p-8 rounded-3xl shadow-2xl border border-white/20 text-center">
            <div className="w-16 h-16 bg-red-500/20 rounded-full flex items-center justify-center mx-auto mb-4">
              <svg className="w-8 h-8 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
            <h2 className="text-2xl font-bold text-white mb-2">เกิดข้อผิดพลาด</h2>
            <p className="text-white/80 mb-4">{error}</p>
            <button
              onClick={() => window.location.reload()}
              className="px-6 py-2 bg-white/20 hover:bg-white/30 text-white rounded-xl transition-all duration-300"
            >
              ลองใหม่
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      {/* Header */}
      <div className="bg-white/5 backdrop-blur-lg border-b border-white/10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center">
              <h1 className="text-2xl font-bold text-white">Team Dashboard</h1>
            </div>
            <div className="flex items-center space-x-4">
              {currentUser && (
                <div className="flex items-center space-x-3">
                  <Image
                    src={currentUser.discordAvatar}
                    alt="Your Avatar"
                    width={32}
                    height={32}
                    className="rounded-full border-2 border-white/20"
                  />
                  <span className="text-white font-medium">{currentUser.discordName}</span>
                </div>
              )}
              <button
                onClick={handleLogout}
                disabled={logoutLoading}
                className="inline-flex items-center px-4 py-2 bg-red-500/20 hover:bg-red-500/30 text-red-400 hover:text-red-300 rounded-xl transition-all duration-300 border border-red-500/20 disabled:opacity-50"
              >
                {logoutLoading ? (
                  <>
                    <div className="w-4 h-4 border-2 border-red-400/30 border-t-red-400 rounded-full animate-spin mr-2"></div>
                    กำลังออกจากระบบ...
                  </>
                ) : (
                  <>
                    <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                    </svg>
                    ออกจากระบบ
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
            <div className="flex items-center">
              <div className="w-12 h-12 bg-blue-500/20 rounded-full flex items-center justify-center mr-4">
                <svg className="w-6 h-6 text-blue-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                </svg>
              </div>
              <div>
                <p className="text-white/70 text-sm">สมาชิกทั้งหมด</p>
                <p className="text-2xl font-bold text-white">{teamMembers.length}</p>
              </div>
            </div>
          </div>

          <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
            <div className="flex items-center">
              <div className="w-12 h-12 bg-green-500/20 rounded-full flex items-center justify-center mr-4">
                <svg className="w-6 h-6 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </div>
              <div>
                <p className="text-white/70 text-sm">ออนไลน์</p>
                <p className="text-2xl font-bold text-white">
                  {teamMembers.filter(member => member.online).length}
                </p>
              </div>
            </div>
          </div>

          <div className="bg-white/10 backdrop-blur-lg rounded-2xl p-6 border border-white/20">
            <div className="flex items-center">
              <div className="w-12 h-12 bg-purple-500/20 rounded-full flex items-center justify-center mr-4">
                <svg className="w-6 h-6 text-purple-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197m13.5-1a4 4 0 11-8 0 4 4 0 018 0z" />
                </svg>
              </div>
              <div>
                <p className="text-white/70 text-sm">Pre-Join</p>
                <p className="text-2xl font-bold text-white">
                  {teamMembers.filter(member => member.preJoin).length}
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* Team Members Table */}
        <div className="bg-white/10 backdrop-blur-lg rounded-2xl border border-white/20 overflow-hidden">
          <div className="px-6 py-4 border-b border-white/10">
            <h2 className="text-xl font-bold text-white">สมาชิกทีมงาน</h2>
          </div>
          
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-white/5">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-white/70 uppercase tracking-wider">สมาชิก</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-white/70 uppercase tracking-wider">สถานะ</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-white/70 uppercase tracking-wider">IP Address</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-white/70 uppercase tracking-wider">ยืนยันตัวตน</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-white/70 uppercase tracking-wider">ระยะเวลา</th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-white/70 uppercase tracking-wider">ทีมงาน</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/10">
                {teamMembers.map((member) => {
                  const verifiedTime = formatVerifiedTime(member.verified_timestamp);
                  return (
                    <tr key={`${member.discord_id}-${member.playerName}`} className="hover:bg-white/5 transition-colors">
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex items-center">
                          <div className="relative">
                            <Image
                              src={member.discordAvatar}
                              alt={member.discordName}
                              width={40}
                              height={40}
                              className="rounded-full border-2 border-white/20"
                            />
                            <div className={`absolute -bottom-1 -right-1 w-4 h-4 rounded-full border-2 border-slate-900 ${member.online ? 'bg-green-500' : 'bg-red-500'}`}></div>
                          </div>
                          <div className="ml-4">
                            <div className="text-sm font-medium text-white">{member.discordName}</div>
                            <div className="text-sm text-white/60">{member.playerName}</div>
                          </div>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="flex space-x-2">
                          <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${member.online ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'}`}>
                            {member.online ? 'ออนไลน์' : 'ออฟไลน์'}
                          </span>
                          <span className={`inline-flex px-2 py-1 text-xs font-semibold rounded-full ${member.preJoin ? 'bg-purple-500/20 text-purple-400' : 'bg-gray-500/20 text-gray-400'}`}>
                            {member.preJoin ? 'Pre-Join' : 'รอ'}
                          </span>
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <span className="text-sm text-white/80 font-mono bg-white/10 px-2 py-1 rounded">
                          {member.ip_address}
                        </span>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-white/80">
                          {typeof verifiedTime === 'string' ? verifiedTime : verifiedTime.formatted}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                        <div className="text-sm text-white/60">
                          {typeof verifiedTime === 'string' ? '-' : verifiedTime.ago}
                        </div>
                      </td>
                      <td className="px-6 py-4 whitespace-nowrap">
                            <button
                                onClick={() => handleForceLogout(member.discord_id)}
                                disabled={logoutLoading}
                                className="inline-flex items-center px-4 py-2 bg-red-500/20 hover:bg-red-500/30 text-red-400 hover:text-red-300 rounded-xl transition-all duration-300 border border-red-500/20 disabled:opacity-50"
                            >
                                {logoutLoading ? (
                                <>
                                    <div className="w-4 h-4 border-2 border-red-400/30 border-t-red-400 rounded-full animate-spin mr-2"></div>
                                    กำลังออกจากระบบ...
                                </>
                                ) : (
                                <>
                                    <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
                                    </svg>
                                    ออกจากระบบ
                                </>
                                )}
                            </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>

        {/* Auto-refresh indicator */}
        <div className="mt-6 text-center">
          <p className="text-white/60 text-sm">
            ข้อมูลจะอัพเดทอัตโนมัติทุก 30 วินาที
          </p>
          <div className="flex justify-center mt-2">
            <div className="w-2 h-2 bg-white/40 rounded-full animate-pulse mx-1"></div>
            <div className="w-2 h-2 bg-white/40 rounded-full animate-pulse mx-1 animation-delay-200"></div>
            <div className="w-2 h-2 bg-white/40 rounded-full animate-pulse mx-1 animation-delay-400"></div>
          </div>
        </div>
      </div>
    </div>
  );
}