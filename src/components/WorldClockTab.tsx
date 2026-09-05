import React, { useState, useEffect } from 'react';
import { Plus, Search, Trash2, Sun, Moon, Globe } from 'lucide-react';

interface CityTime {
  id: string;
  name: string;
  country: string;
  timezone: string;
  offsetHours: number;
}

const DEFAULT_CITIES: CityTime[] = [
  { id: 'msk', name: 'Москва', country: 'Россия', timezone: 'Europe/Moscow', offsetHours: 3 },
  { id: 'lon', name: 'Лондон', country: 'Великобритания', timezone: 'Europe/London', offsetHours: 1 },
  { id: 'nyc', name: 'Нью-Йорк', country: 'США', timezone: 'America/New_York', offsetHours: -4 },
  { id: 'tok', name: 'Токио', country: 'Япония', timezone: 'Asia/Tokyo', offsetHours: 9 },
  { id: 'par', name: 'Париж', country: 'Франция', timezone: 'Europe/Paris', offsetHours: 2 },
];

const AVAILABLE_CITIES: CityTime[] = [
  { id: 'syd', name: 'Сидней', country: 'Австралия', timezone: 'Australia/Sydney', offsetHours: 10 },
  { id: 'dub', name: 'Дубай', country: 'ОАЭ', timezone: 'Asia/Dubai', offsetHours: 4 },
  { id: 'ber', name: 'Берлин', country: 'Германия', timezone: 'Europe/Berlin', offsetHours: 2 },
  { id: 'sin', name: 'Сингапур', country: 'Сингапур', timezone: 'Asia/Singapore', offsetHours: 8 },
  { id: 'lax', name: 'Лос-Анджелес', country: 'США', timezone: 'America/Los_Angeles', offsetHours: -7 },
  { id: 'pek', name: 'Пекин', country: 'Китай', timezone: 'Asia/Shanghai', offsetHours: 8 },
  { id: 'ist', name: 'Стамбул', country: 'Турция', timezone: 'Europe/Istanbul', offsetHours: 3 },
];

export const WorldClockTab: React.FC = () => {
  const [time, setTime] = useState(new Date());
  const [cities, setCities] = useState<CityTime[]>(DEFAULT_CITIES);
  const [showAddModal, setShowAddModal] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    const timer = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(timer);
  }, []);

  const formatCityTime = (city: CityTime) => {
    try {
      const cityDate = new Date(
        time.toLocaleString('en-US', { timeZone: city.timezone })
      );
      const h = cityDate.getHours();
      const m = cityDate.getMinutes();
      const isNight = h < 6 || h >= 21;
      const formattedTime = `${h < 10 ? '0' : ''}${h}:${m < 10 ? '0' : ''}${m}`;
      
      const homeOffset = -time.getTimezoneOffset() / 60;
      const diff = city.offsetHours - homeOffset;
      const diffStr = diff === 0 
        ? 'Местное время' 
        : diff > 0 
          ? `на ${diff} ч позже` 
          : `на ${Math.abs(diff)} ч раньше`;

      return { formattedTime, isNight, diffStr };
    } catch {
      return { formattedTime: '--:--', isNight: false, diffStr: '' };
    }
  };

  const handleAddCity = (city: CityTime) => {
    if (!cities.some(c => c.id === city.id)) {
      setCities([...cities, city]);
    }
    setShowAddModal(false);
    setSearchQuery('');
  };

  const handleRemoveCity = (id: string) => {
    setCities(cities.filter(c => c.id !== id));
  };

  const filteredCities = AVAILABLE_CITIES.filter(
    c => !cities.some(existing => existing.id === c.id) &&
         (c.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          c.country.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  const hours = time.getHours();
  const minutes = time.getMinutes();
  const seconds = time.getSeconds();
  const timeFormatted = `${hours < 10 ? '0' : ''}${hours}:${minutes < 10 ? '0' : ''}${minutes}`;
  const secondsFormatted = `${seconds < 10 ? '0' : ''}${seconds}`;

  const dateFormatted = time.toLocaleDateString('ru-RU', {
    weekday: 'long',
    day: 'numeric',
    month: 'long'
  });

  return (
    <div className="flex-1 flex flex-col overflow-hidden bg-neutral-950 text-neutral-100">
      {/* Top Header */}
      <div className="px-5 pt-3 pb-2 flex items-center justify-between border-b border-neutral-900 shrink-0">
        <div>
          <h1 className="text-lg font-bold text-neutral-100 tracking-tight">Часы</h1>
          <p className="text-[11px] text-neutral-400 capitalize">{dateFormatted}</p>
        </div>
        <button
          id="btn-add-world-city"
          onClick={() => setShowAddModal(true)}
          className="w-9 h-9 rounded-full bg-blue-600 hover:bg-blue-500 text-white flex items-center justify-center transition shadow-md shadow-blue-600/30 active:scale-95"
          title="Добавить город"
        >
          <Plus className="w-5 h-5" />
        </button>
      </div>

      {/* Main Clock Hero */}
      <div className="px-5 py-4 flex flex-col items-center justify-center border-b border-neutral-900/80 bg-neutral-900/30 shrink-0">
        <div className="flex items-baseline gap-1 font-mono font-bold tracking-tight text-white">
          <span className="text-4xl text-neutral-100">{timeFormatted}</span>
          <span className="text-sm font-semibold text-blue-400 font-mono w-6">{secondsFormatted}</span>
        </div>
        <div className="flex items-center gap-1.5 mt-1 text-xs text-neutral-400 font-medium">
          <Globe className="w-3.5 h-3.5 text-blue-400" />
          <span>Текущее время (Ваш регион)</span>
        </div>
      </div>

      {/* World Cities List */}
      <div className="flex-1 overflow-y-auto px-4 py-3 space-y-2.5 scrollbar-thin scrollbar-thumb-neutral-800">
        {cities.map(city => {
          const { formattedTime, isNight, diffStr } = formatCityTime(city);
          return (
            <div
              key={city.id}
              className="p-3.5 rounded-2xl bg-neutral-900/80 border border-neutral-800/80 hover:border-neutral-700 transition flex items-center justify-between"
            >
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-sm font-semibold text-neutral-100">{city.name}</span>
                  {isNight ? (
                    <Moon className="w-3.5 h-3.5 text-indigo-400" />
                  ) : (
                    <Sun className="w-3.5 h-3.5 text-amber-400" />
                  )}
                </div>
                <div className="text-[11px] text-neutral-400 mt-0.5">
                  {city.country} • <span className="text-neutral-500">{diffStr}</span>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <div className="text-2xl font-mono font-bold text-neutral-100">
                  {formattedTime}
                </div>
                <button
                  onClick={() => handleRemoveCity(city.id)}
                  className="p-1 text-neutral-500 hover:text-red-400 transition"
                  title="Удалить"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Add City Modal */}
      {showAddModal && (
        <div className="absolute inset-0 z-40 bg-black/80 backdrop-blur-sm p-4 flex flex-col justify-end">
          <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-4 max-h-[85%] flex flex-col">
            <div className="flex items-center justify-between pb-3 border-b border-neutral-800">
              <h3 className="font-semibold text-sm text-neutral-100">Добавить город</h3>
              <button
                onClick={() => setShowAddModal(false)}
                className="text-xs text-neutral-400 hover:text-white"
              >
                Закрыть
              </button>
            </div>

            <div className="mt-3 relative">
              <Search className="w-4 h-4 text-neutral-500 absolute left-3 top-2.5" />
              <input
                type="text"
                placeholder="Поиск города..."
                value={searchQuery}
                onChange={e => setSearchQuery(e.target.value)}
                className="w-full pl-9 pr-3 py-2 bg-neutral-950 border border-neutral-800 rounded-xl text-xs text-white placeholder-neutral-500 focus:outline-none focus:border-blue-500"
              />
            </div>

            <div className="flex-1 overflow-y-auto mt-3 space-y-1.5">
              {filteredCities.map(city => (
                <button
                  key={city.id}
                  onClick={() => handleAddCity(city)}
                  className="w-full text-left px-3 py-2 rounded-xl hover:bg-neutral-800 text-xs flex items-center justify-between transition"
                >
                  <span className="font-medium text-neutral-200">{city.name}, {city.country}</span>
                  <span className="text-neutral-500 text-[10px]">UTC{city.offsetHours >= 0 ? `+${city.offsetHours}` : city.offsetHours}</span>
                </button>
              ))}
              {filteredCities.length === 0 && (
                <p className="text-center py-4 text-xs text-neutral-500">Городов не найдено</p>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
